/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.matchers;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

//stolen from hamcrest because I didn't want to have more dependency than Matcher class
public class Equality {
    /**
     * 白名单成员
     * 对象的成员变量是否在这里面,如果在,自动跳过
     * 什么情况往这里加数据? 对象某些变量生成规则是随机数或者按照当前时间生成的字符串等本地无法模拟的信息
     */
    private static List<String> WRHITE_LIST_FIELDS=new ArrayList<String>();

    public static void addField(String field){
        if(WRHITE_LIST_FIELDS==null){
            WRHITE_LIST_FIELDS=new ArrayList<String>();
        }
        WRHITE_LIST_FIELDS.add(field);
    }

    public static boolean areEqual(Object o1, Object o2) {
        if (o1 == o2 ) {
            return true;
        } else if (o1 == null || o2 == null) {
            return false;
        } else if (isArray(o1)) {
            return isArray(o2) && areArraysEqual(o1, o2);
        } else {
            //对象比较 改成遍历对象的属性进行比较 跳过date类型
            //return o1.equals(o2);
            return equals(o1,o2);
        }
    }
    private static Boolean equals(Object o1, Object o2){
        //对象的处理
        Class<?> aClass = o1.getClass();
        if(o1 instanceof Date && o2 instanceof Date){
            //date类型不比较了 全都放行;很难判断当前时间是程序生成的还是数据库查到的, 程序生成的存在时区问题,当前时间问题,干脆不管了
            //return true;
            //绕不开这个,还是要判断时间 要么当前时间相同,要么o2+8小时等于当前时间   什么时候不判断时间,测试环境取当前时间, 开发也取当前时间肯定对不上,直接date类型没法跳过,下面对象可以跳过
            Calendar instance = Calendar.getInstance();
            instance.setTime((Date) o2);
            instance.add(Calendar.HOUR_OF_DAY,8);
            return o1.equals(o2)|| o1.equals(instance.getTime());
        }
        if(aClass.getName().startsWith("com.aku")){
            return itemEquals( aClass, o1,  o2);
        }
        if(o1 instanceof List){
            return areListEqual(o1,o2);
        }
        return o1.equals(o2);
    }

    static boolean areListEqual(Object o1, Object o2) {
         if(o1 instanceof LinkedList){
            for(int i=0;i<((LinkedList<?>) o1).size();i++){
                if (!areEqual(((LinkedList<?>) o1).get(i), ((LinkedList<?>) o2).get(i))) {
                    return false;
                }
            }
            return true;
        }
        for(int i=0;i<((List) o1).size();i++){
            if (!areEqual(((List) o1).get(i), ((List) o2).get(i))) {
                return false;
            }
        }
        return true;
    }

    private static Boolean itemEquals(Class<?> aClass,Object o1, Object o2){
        Boolean equalsId=false;
        for (Field t : aClass.getDeclaredFields()) {
            try {
                //对象内的date也不跳过了, 根据 WRHITE_LIST_FIELDS 控制哪些跳过哪些不跳
//                if (Objects.equals(t.getType().getName(), "java.util.Date")) {
//                    if(FORCE_DATE_FIELDS.contains(t.getName())){
//
//                    }
//                    continue;
//                }

                if(WRHITE_LIST_FIELDS.contains(t.getType().getName())){
                    System.out.println("类型:("+t.getType().getName()+")匹配跳过");
                    continue;
                }
                if(WRHITE_LIST_FIELDS.contains(t.getName())){
                    System.out.println("字段:("+t.getName()+")匹配跳过");
                    continue;
                }

                Method method = aClass.getMethod("get" + upperCase1th(t.getName()), null);
                Object invokeO1 = method.invoke(o1);
                Object invokeO2 = method.invoke(o2);
                //如果是list对象
                boolean equals = areEqual(invokeO1, invokeO2);
                if(Objects.equals("id",t.getName())){
                    equalsId=equals;
                }
                //false 直接跳出
                if (!equals) {
                    //尽可能多给些提示 对象比较的时候, 在id相同的情况下(常规大部分对象都有id) 其他字段对不上, 那这个字段可能是不可控字段 打印出来让用户判断
                    printErrorField(aClass,invokeO1,invokeO2,t,equalsId);
                    return equals;
                }
            } catch (Exception e) {
                System.out.println(aClass.getName() + "的成员" + t.getName() + "error :"+e.getMessage());
            }
        }

        return true;
    }
    private static void printErrorField(Class<?> aClass,Object invokeO1, Object invokeO2,Field t,Boolean equalsId){
        if(equalsId){
            System.out.println("类:("+aClass.getName()+"),字段:("+t.getName()+")匹配失败,本地值:("+invokeO2.toString()+"),日志值:("+invokeO1.toString()+")####如有必要可配置跳过,trace.wrhiteListFields="+t.getName());
        }
    }

    private static String upperCase1th(String str){
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }


    static boolean areArraysEqual(Object o1, Object o2) {
        return areArrayLengthsEqual(o1, o2)
            && areArrayElementsEqual(o1, o2);
    }

    static boolean areArrayLengthsEqual(Object o1, Object o2) {
        return Array.getLength(o1) == Array.getLength(o2);
    }

    static boolean areArrayElementsEqual(Object o1, Object o2) {
        for (int i = 0; i < Array.getLength(o1); i++) {
            if (!areEqual(Array.get(o1, i), Array.get(o2, i))) return false;
        }
        return true;
    }

    static boolean isArray(Object o) {
        return o.getClass().isArray();
    }
}
