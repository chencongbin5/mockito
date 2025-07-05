/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.internal.invocation;

import static org.mockito.internal.invocation.ArgumentsProcessor.argumentsToMatchers;
import static org.mockito.internal.invocation.MatcherApplicationStrategy.getMatcherApplicationStrategyFor;
import static org.mockito.internal.invocation.TypeSafeMatching.matchesTypeSafe;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.CapturesArguments;
import org.mockito.internal.reporting.PrintSettings;
import org.mockito.invocation.DescribedInvocation;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.Location;
import org.mockito.invocation.MatchableInvocation;

/**
 * In addition to all content of the invocation, the invocation matcher contains the argument matchers. Invocation matcher is used during verification and stubbing. In those cases, the user can provide argument matchers instead of 'raw' arguments. Raw arguments are converted to 'equals' matchers anyway.
 */
@SuppressWarnings("serial")
public class InvocationMatcher implements MatchableInvocation, DescribedInvocation, Serializable {

    private final Invocation invocation;
    private final List<ArgumentMatcher<?>> matchers;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public InvocationMatcher(Invocation invocation, List<ArgumentMatcher> matchers) {
        this.invocation = invocation;
        if (matchers.isEmpty()) {
            this.matchers = (List) argumentsToMatchers(invocation.getArguments());
        } else {
            this.matchers = (List) matchers;
        }
    }

    @SuppressWarnings("rawtypes")
    public InvocationMatcher(Invocation invocation) {
        this(invocation, Collections.<ArgumentMatcher> emptyList());
    }

    public static List<InvocationMatcher> createFrom(List<Invocation> invocations) {
        LinkedList<InvocationMatcher> out = new LinkedList<InvocationMatcher>();
        for (Invocation i : invocations) {
            out.add(new InvocationMatcher(i));
        }
        return out;
    }

    public Method getMethod() {
        return invocation.getMethod();
    }

    @Override
    public Invocation getInvocation() {
        return invocation;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<ArgumentMatcher> getMatchers() {
        return (List) matchers;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public String toString() {
        return new PrintSettings().print((List) matchers, invocation);
    }

    @Override
    public boolean matches(Invocation candidate) {
        return invocation.getMock().equals(candidate.getMock()) && hasSameMethod(candidate) && argumentsMatch(candidate);
    }

    /**
     * similar means the same method name, same mock, unverified and: if arguments are the same cannot be overloaded
     */
    @Override
    public boolean hasSimilarMethod(Invocation candidate) {
        String wantedMethodName = getMethod().getName();
        String candidateMethodName = candidate.getMethod().getName();

        if (!wantedMethodName.equals(candidateMethodName)) {
            return false;
        }
        if (candidate.isVerified()) {
            return false;
        }
        if (getInvocation().getMock() != candidate.getMock()) {
            return false;
        }
        if (hasSameMethod(candidate)) {
            return true;
        }

        return !argumentsMatch(candidate);
    }

    @Override
    public boolean hasSameMethod(Invocation candidate) {
        // not using method.equals() for 1 good reason:
        // sometimes java generates forwarding methods when generics are in play see JavaGenericsForwardingMethodsTest
        Method m1 = invocation.getMethod();
        Method m2 = candidate.getMethod();

        if (m1.getName() != null && m1.getName().equals(m2.getName())) {
            /* Avoid unnecessary cloning */
            Class<?>[] params1 = m1.getParameterTypes();
            Class<?>[] params2 = m2.getParameterTypes();
            return Arrays.equals(params1, params2);
        }
        return false;
    }

    @Override
    public Location getLocation() {
        return invocation.getLocation();
    }

    @Override
    public void captureArgumentsFrom(Invocation invocation) {
        MatcherApplicationStrategy strategy = getMatcherApplicationStrategyFor(invocation, matchers);
        strategy.forEachMatcherAndArgument(captureArgument());
    }

    private ArgumentMatcherAction captureArgument() {
        return new ArgumentMatcherAction() {

            @Override
            public boolean apply(ArgumentMatcher<?> matcher, Object argument) {
                if (matcher instanceof CapturesArguments) {
                    ((CapturesArguments) matcher).captureFrom(argument);
                }

                return true;
            }
        };
    }

    public static List<String> MATCH_SUCCESS_RECORD;
    public static List<String> MATCH_FAILED_RECORD;

    public static void matchRecordInit(){
        MATCH_SUCCESS_RECORD=new ArrayList<String>();
        MATCH_FAILED_RECORD=new ArrayList<String>();
    }
    public static void mathRecordLog(int count){
        System.out.println("mock 方法成功率:"+ BigDecimal.valueOf(InvocationMatcher.MATCH_SUCCESS_RECORD.size()).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(count),2, RoundingMode.HALF_UP)+"%");
        System.out.println("mock 符合预期数:"+InvocationMatcher.MATCH_SUCCESS_RECORD.size());
        for (String t:InvocationMatcher.MATCH_SUCCESS_RECORD){
            System.out.println("mock 成功的方法:"+t);
        }
//        System.out.println("mock 不符合预期数:"+InvocationMatcher.MATCH_FAILED_RECORD.size());
//        for (String t:InvocationMatcher.MATCH_FAILED_RECORD){
//            System.out.println("mock 失败的方法:"+t);
//        }
        //不符合预期 有两种情况 1.就是匹配失败,2同一个方法mock了多次,匹配A失败,匹配B成功,总的来说是成功了,输出下列信息是方便情况1失败原因,成功率100%时候 下列输出可以不看
    }
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean argumentsMatch(Invocation actual) {
        List matchers = getMatchers();
        boolean flag = getMatcherApplicationStrategyFor(actual, matchers).forEachMatcherAndArgument(matchesTypeSafe());
        if(flag && MATCH_SUCCESS_RECORD!=null){
            MATCH_SUCCESS_RECORD.add(""+ actual.getMethod());
        }else if(!flag && MATCH_FAILED_RECORD!=null){
            MATCH_FAILED_RECORD.add(""+ actual.getMethod());
        }
        return flag;
    }

}
