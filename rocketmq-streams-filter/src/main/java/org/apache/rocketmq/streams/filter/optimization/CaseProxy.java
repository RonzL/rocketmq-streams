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

package org.apache.rocketmq.streams.filter.optimization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.rocketmq.streams.common.context.IMessage;
import org.apache.rocketmq.streams.common.optimization.cachefilter.ICacheFilter;
import org.apache.rocketmq.streams.filter.FilterComponent;
import org.apache.rocketmq.streams.filter.builder.ExpressionBuilder;
import org.apache.rocketmq.streams.filter.function.script.CaseFunction;
import org.apache.rocketmq.streams.filter.operator.Rule;
import org.apache.rocketmq.streams.filter.operator.expression.Expression;
import org.apache.rocketmq.streams.filter.operator.expression.SimpleExpression;
import org.apache.rocketmq.streams.script.context.FunctionContext;
import org.apache.rocketmq.streams.script.optimization.performance.AbstractScriptProxy;
import org.apache.rocketmq.streams.script.service.IScriptExpression;
import org.apache.rocketmq.streams.script.service.IScriptParamter;

public class CaseProxy extends AbstractScriptProxy {
    protected Rule rule;
    public CaseProxy(IScriptExpression origExpression) {
        super(origExpression);
    }

    @Override public List<ICacheFilter> getCacheFilters() {
        List<IScriptParamter> parameters = this.origExpression.getScriptParamters();
        IScriptParamter scriptParamter=parameters.get(0);
        String expressionStr=getParameterValue(scriptParamter);

        this.rule=ExpressionBuilder.createRule("tmp","tmp",expressionStr);
        Collection<Expression> expressionSet = this.rule.getExpressionMap().values();
        List<ICacheFilter> cacheFilters=new ArrayList<>();
        for(Expression expression:expressionSet){
            if(expression instanceof SimpleExpression|| expression instanceof Expression){
                AbstractExpressionProxy expressionProxy= ExpressionProxyFactory.getInstance().create(expression,rule);
                if(expressionProxy!=null){
                    this.rule.getExpressionMap().put(expressionProxy.getConfigureName(),expressionProxy);
                    if(expressionProxy instanceof ICacheFilter){
                        cacheFilters.add((ICacheFilter)expressionProxy);
                    }
                }
            }
        }
        return cacheFilters;
    }

    @Override
    public boolean supportOptimization(IScriptExpression scriptExpression) {
        String functionName = scriptExpression.getFunctionName();
        boolean match = CaseFunction.isCaseFunction(functionName);
        if (match) {
            List<IScriptParamter> parameters = this.origExpression.getScriptParamters();
            IScriptParamter scriptParamter=parameters.get(0);
            String expressionStr=getParameterValue(scriptParamter);
            if (expressionStr == null) {
                return false;
            }
            if (expressionStr.length() <= 5) {
                String lowValue = expressionStr.trim().toLowerCase();
                if (lowValue.equals("true") || lowValue.equals("false")) {
                   return false;
                }
            }
            if (expressionStr.startsWith("(") && expressionStr.endsWith(")")) {
                return true;
            }
        }
        return false;
    }

    @Override public Object executeExpression(IMessage message, FunctionContext context) {
        FilterComponent filterComponent=FilterComponent.getInstance();
        List<Rule> fireRules=filterComponent.executeRule(message,context,this.rule);
        boolean isFired= fireRules!=null&&fireRules.size()>0;
        if(origExpression.getNewFieldNames()!=null&&origExpression.getNewFieldNames().size()>0){
            message.getMessageBody().put(origExpression.getNewFieldNames().iterator().next(),isFired);
        }
        return isFired;
    }
}
