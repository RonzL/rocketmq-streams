/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.streams.common.topology.builder;

import org.apache.rocketmq.streams.common.channel.sink.ISink;
import org.apache.rocketmq.streams.common.channel.source.ISource;
import org.apache.rocketmq.streams.common.configurable.AbstractConfigurable;
import org.apache.rocketmq.streams.common.configurable.IConfigurable;
import org.apache.rocketmq.streams.common.configurable.IConfigurableService;
import org.apache.rocketmq.streams.common.metadata.MetaData;
import org.apache.rocketmq.streams.common.topology.ChainPipeline;
import org.apache.rocketmq.streams.common.topology.ChainStage;
import org.apache.rocketmq.streams.common.topology.model.Pipeline;
import org.apache.rocketmq.streams.common.topology.stages.OutputChainStage;
import org.apache.rocketmq.streams.common.utils.NameCreatorUtil;
import org.apache.rocketmq.streams.common.utils.StringUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PipelineBuilder implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 最终产出的pipeline
     */
    protected ChainPipeline pipeline = new ChainPipeline();

    /**
     * 保存pipeline构建过程中产生的configurable对象
     */
    protected List<IConfigurable> configurables = new ArrayList<>();

    /**
     * pipeline namespace
     */
    protected String pipelineNameSpace;

    /**
     * pipeline name
     */
    protected String pipelineName;

    protected MetaData channelMetaData;//数据源的格式，非必须

    /**
     * 如果需要制作拓扑结构，则保存当前构建的stage
     */
    protected ChainStage currentChainStage;

    protected String parentTableName;//在sql tree中，存储当前节点父节点的table name，主要用于双流join场景，用于判断是否是右流join
    protected boolean isBreak = false;//设置这个值后，后面的所有逻辑都不再继续

    public PipelineBuilder(String namespace, String pipelineName) {
        pipeline.setNameSpace(namespace);
        pipeline.setConfigureName(pipelineName);
        this.pipelineNameSpace = namespace;
        this.pipelineName = pipelineName;
        addConfigurables(pipeline);
    }

    /**
     * 设置pipeline的source
     *
     * @param source 数据源
     */
    public void setSource(ISource source) {
        source.createStageChain(this);
        source.addConfigurables(this);
        this.pipeline.setSource(source);
    }

    /**
     * 创建chain stage
     *
     * @param stageBuilder
     * @return
     */
    public ChainStage createStage(IStageBuilder<ChainStage> stageBuilder) {
        ChainStage chainStage = stageBuilder.createStageChain(this);
        stageBuilder.addConfigurables(this);// 这句一定要在addChainStage前，会默认赋值namespace和name
        if (StringUtil.isEmpty(chainStage.getLabel())) {
            chainStage.setLabel(createConfigurableName(chainStage.getType()));
        }
        this.pipeline.addChainStage(chainStage);

        return chainStage;
    }

    public List<String> createSQL() {
        List<String> sqls = new ArrayList<>();
        for (IConfigurable configurable : configurables) {
            AbstractConfigurable abstractConfigurable = (AbstractConfigurable) configurable;
            sqls.add(AbstractConfigurable.createSQL(configurable));
        }
        return sqls;
    }

    public ChainPipeline build(IConfigurableService configurableService) {
        List<IConfigurable> configurableList = configurables;
        pipeline.setChannelMetaData(channelMetaData);
        if (configurableList != null) {
            for (IConfigurable configurable : configurableList) {
                configurableService.insert(configurable);
            }
        }
        configurableService.refreshConfigurable(pipelineNameSpace);
        ChainPipeline pipline = configurableService.queryConfigurable(Pipeline.TYPE, pipelineName);
        return pipline;
    }

    public List<IConfigurable> getAllConfigurables() {
        List<IConfigurable> configurableList = configurables;
        pipeline.setChannelMetaData(channelMetaData);
        return configurableList;
    }

    /**
     * 创建chain stage
     *
     * @param sink
     * @return
     */
    public ChainStage createStage(ISink sink) {
        OutputChainStage outputChainStage = new OutputChainStage();
        sink.addConfigurables(this);
        outputChainStage.setSink(sink);
        if (StringUtil.isEmpty(sink.getConfigureName())) {
            sink.setConfigureName(createConfigurableName(sink.getType()));
        }
        pipeline.addChainStage(outputChainStage);
        return outputChainStage;
    }

    /**
     * 增加输出
     *
     * @param sink
     * @return
     */
    public OutputChainStage addOutput(ISink sink) {
        if (isBreak) {
            return null;
        }
        OutputChainStage outputChainStage = new OutputChainStage();
        sink.addConfigurables(this);
        outputChainStage.setSink(sink);
        pipeline.addChainStage(outputChainStage);
        return outputChainStage;
    }

    /**
     * 增加维表
     *
     * @param configurable
     */
    public void addNameList(IConfigurable configurable) {
        if (isBreak) {
            return;
        }
        addConfigurables(configurable);
    }

    /**
     * 增加中间chain stage
     *
     * @param stageChainBuilder
     */
    public ChainStage addChainStage(IStageBuilder<ChainStage> stageChainBuilder) {
        if (isBreak) {
            return null;
        }
        ChainStage chainStage = stageChainBuilder.createStageChain(this);
        stageChainBuilder.addConfigurables(this);// 这句一定要在addChainStage前，会默认赋值namespace和name
        pipeline.addChainStage(chainStage);
        return chainStage;
    }

    /**
     * 自动创建组建名称
     *
     * @param type
     * @return
     */
    public String createConfigurableName(String type) {
        return NameCreatorUtil.createNewName(this.pipelineName, type);
    }

    /**
     * 保存中间产生的结果
     */
    public void addConfigurables(IConfigurable configurable) {
        if (isBreak) {
            return;
        }
        if (configurable != null) {
            if (StringUtil.isEmpty(configurable.getNameSpace())) {
                configurable.setNameSpace(getPipelineNameSpace());
            }
            if (StringUtil.isEmpty(configurable.getConfigureName())) {
                configurable.setConfigureName(createConfigurableName(configurable.getType()));
            }
            //判断配置信息是否已经存在，如果不存在，则添加
            for (IConfigurable config : this.configurables) {
                if (config.getType().equals(configurable.getType()) && config.getConfigureName().equals(configurable.getConfigureName())) {
                    return;
                }
            }
            this.configurables.add(configurable);
        }
    }

    public void addConfigurables(Collection<? extends IConfigurable> configurables) {
        if (isBreak) {
            return;
        }
        if (configurables != null) {
            for (IConfigurable configurable : configurables) {
                addConfigurables(configurable);
            }
        }
    }

    /**
     * 在当前拓扑基础上，增加下一层级的拓扑。如果需要做拓扑，需要设置标签
     *
     * @param nextStages
     */
    public void setTopologyStages(ChainStage currentChainStage, List<ChainStage> nextStages) {
        if (isBreak) {
            return;
        }
        if (nextStages == null) {
            return;
        }
        List<String> lableNames = new ArrayList<>();
        for (ChainStage stage : nextStages) {
            lableNames.add(stage.getLabel());
        }

        if (currentChainStage == null) {
            this.pipeline.setChannelNextStageLabel(lableNames);
        } else {
            currentChainStage.setNextStageLabels(lableNames);
        }
    }

    /**
     * 拓扑的特殊形式，下层只有单个节点
     *
     * @param nextStage
     */
    public void setTopologyStages(ChainStage currentChainStage, ChainStage nextStage) {
        if (isBreak) {
            return;
        }
        List<ChainStage> stages = new ArrayList<>();
        stages.add(nextStage);
        setTopologyStages(currentChainStage, stages);
    }

    public String getPipelineNameSpace() {
        return pipelineNameSpace;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public ChainPipeline getPipeline() {
        return pipeline;
    }

    public void setHorizontalStages(ChainStage stage) {
        if (isBreak) {
            return;
        }
        List<ChainStage> stages = new ArrayList<>();
        stages.add(stage);
        setHorizontalStages(stages);
    }

    /**
     * 如果需要做拓扑，需要设置标签
     *
     * @param stages
     */
    public void setHorizontalStages(List<ChainStage> stages) {
        if (isBreak) {
            return;
        }
        if (stages == null) {
            return;
        }
        List<String> lableNames = new ArrayList<>();
        Map<String, ChainStage> lableName2Stage = new HashMap();
        for (ChainStage stage : stages) {
            lableNames.add(stage.getLabel());
            lableName2Stage.put(stage.getLabel(), stage);
        }

        if (currentChainStage == null) {
            this.pipeline.setChannelNextStageLabel(lableNames);
            for (String lableName : lableNames) {
                ChainStage chainStage = lableName2Stage.get(lableName);
                chainStage.getPrevStageLabels().add(this.pipeline.getChannelName());
            }
        } else {
            currentChainStage.setNextStageLabels(lableNames);
            for (String lableName : lableNames) {
                ChainStage chainStage = lableName2Stage.get(lableName);
                List<String> prewLables = chainStage.getPrevStageLabels();
                if (!prewLables.contains(this.currentChainStage.getLabel())) {
                    chainStage.getPrevStageLabels().add(this.currentChainStage.getLabel());
                }

            }
        }
    }

    public void setCurrentChainStage(ChainStage currentChainStage) {
        this.currentChainStage = currentChainStage;
    }

    public ChainStage getCurrentChainStage() {
        return currentChainStage;
    }

    public List<IConfigurable> getConfigurables() {
        return configurables;
    }

    public MetaData getChannelMetaData() {
        return channelMetaData;
    }

    public void setChannelMetaData(MetaData channelMetaData) {
        this.channelMetaData = channelMetaData;
    }

    public String getParentTableName() {
        return parentTableName;
    }

    public void setParentTableName(String parentTableName) {
        this.parentTableName = parentTableName;
    }

    public boolean isBreak() {
        return isBreak;
    }

    public void setBreak(boolean aBreak) {
        isBreak = aBreak;
    }
}
