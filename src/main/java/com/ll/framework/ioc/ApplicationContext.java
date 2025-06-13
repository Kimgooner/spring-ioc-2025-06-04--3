package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Bean;
import com.ll.framework.ioc.annotations.Component;
import com.ll.framework.ioc.annotations.Configuration;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public class ApplicationContext {
    private final String basePackage;
    private Map<String, Object> beans = new HashMap<>();
    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
    }

    public void init() {
        Reflections reflections = new Reflections(basePackage);

        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Component.class);
        for(Class<?> clazz : classes){
            if(clazz.isInterface()) continue; //인터페이스인 경우 continue.
            if(clazz.isAnnotationPresent(Configuration.class)){ //@Configuration일 경우 내부의 @Bean을 스캔.
                Method[] methods = clazz.getDeclaredMethods();
                for(Method method : methods){
                    genBean(clazz, method);
                }
                continue;
            }
            if(!beans.containsKey(extractClassName(clazz))) beans.put(extractClassName(clazz), genBean(clazz));
        }

        System.out.println(beans.toString());
    }

    public <T> T genBean(String beanName) { // beans 에서 인스턴스 추출
        return (T) beans.get(makeClassName(beanName));
    }

    public Object genBean(Class<?> clazz, Method method){ // Reflection, Method로 부터 빈 생성
        String beanName = method.getName();
        List<Object> input_parameters = new ArrayList<>();

        for(Class<?> param : method.getParameterTypes()){
            input_parameters.add(genBean(param));
        }

        try {
            Object instance = method.invoke(genBean(clazz), input_parameters.toArray()); // invoke 메소드로 인스턴스 리턴
            beans.put(makeClassName(beanName), instance);
            return instance;
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(method.getParameterCount());
        System.out.println("구분");
        return null;
    }

    public Object genBean(Class<?> clazz){ // Reflection, Class로 부터 빈 생성
        String className = extractClassName(clazz);
        if(beans.containsKey(className)){
            return beans.get(className);
        }

        Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
        List<Object> input_parameters = new ArrayList<>();

        for(Class<?> param : constructor.getParameterTypes()){
            input_parameters.add(genBean(param));
        }

        try {
            Object instance = constructor.newInstance(input_parameters.toArray());
            beans.put(className, instance);
            return instance;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("빈 생성 중 문제가 발생했습니다.");
        }
        return null;
    }

    public String makeClassName(String beanName){
        return beanName.substring(0,1).toUpperCase() + beanName.substring(1);
    }

    public String extractClassName(Class<?> clazz){
        return clazz.getName().replace(clazz.getPackageName() + ".", "");
    }
}
