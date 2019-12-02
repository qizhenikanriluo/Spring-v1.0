package com.zhur.mvcframework.v2.servlet;

import com.zhur.mvcframework.annotation.GPAutowride;
import com.zhur.mvcframework.annotation.GPController;
import com.zhur.mvcframework.annotation.GPRequestMapping;
import com.zhur.mvcframework.annotation.GPService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @program: spring-1.0
 * @description: 初始化类
 * @author: zhur
 * @date: 2019-11-29 20:04
 **/
public class GPDispatcherServlet extends HttpServlet {

    private Properties properties = new Properties();

    private List<String> className = new ArrayList<String>();

    private Map<String,Object> ioc = new HashMap<String, Object>();

    private Map<String,Method> handleMapping = new HashMap<String, Method>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception,Detail:"+ Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{

        //绝对路径
        String url = req.getRequestURI();
        //处理相对路径
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");
        if (!this.handleMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found!!!");
            return;
        }

        Method method = this.handleMapping.get(url);
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        Map<String,String[]>params = req.getParameterMap();
        method.invoke(ioc.get(beanName),new Object[]{req,resp,params.get("name")[0]} );
    }

    //初始化阶段
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {

        //1、加载配置文件

        doLoadConfig(servletConfig.getInitParameter("contextConfigLocation"));

        //2、扫描相关的类

        doScanner(properties.getProperty("scanPackage"));

        //3、初始化扫描到的类 并将其放到IOC容器初始化

        doInstance();

        //4、完成依赖注入

        doAutowired();

        //5、初始化HanderMapping

        initHanderMapping();


        System.out.println("GP Spring framework is init.");

    }

    //配置文件
    private void doLoadConfig(String servletConfig) {

        //从类路径下找到Spring主配置文件所在类路径
        //将其读出来放到Properties对象中
        InputStream fis =  this.getClass().getClassLoader().getResourceAsStream(servletConfig);
        try {
            properties.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    //扫描到所有的类并获取其类名 -- 存放在数组中
    private void doScanner(String scanPackage) {
        //classPath
        URL url = this.getClass().getClassLoader().
                getResource("/"+scanPackage.replaceAll("\\.","/"));
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            if(file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            }else {
                if(!file.getName().endsWith(".class")){ continue; }
                String clazzName = scanPackage+"."+file.getName().replaceAll(".class","");
                className.add(clazzName);
            }
        }
    }

    private void doInstance() {
        //初始化,为DI作准备
        if (className.isEmpty()){return;}
        for (String className:className) {
            try {
                Class<?> clzz = Class.forName(className);
                //初始化什么样的类   带注解的类 @Service  @Controller
                if (clzz.isAnnotationPresent(GPController.class)){
                    Object instance = clzz.newInstance();
                    //注入IOC容器
                    String beanName = toLowerFirstCase(clzz.getSimpleName());
                    ioc.put(beanName,instance);
                }else if(clzz.isAnnotationPresent(GPService.class)){
                    GPService gpService = clzz.getAnnotation(GPService.class);
                    Object instance = clzz.newInstance();
                   //1、自定义BeanName
                    String beanName = gpService.value();
                   //2、默认类名小写
                    if ("".equals(beanName.trim())) {

                        beanName = toLowerFirstCase(clzz.getSimpleName());
                        ioc.put(beanName,instance);
                    }
                    //3、根据类型自动赋值,投机取巧
                    for (Class i :clzz.getInterfaces()) {
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("This "+ i.getName() +"is  exists!!!");
                        }
                        ioc.put(i.getName(),instance);
                    }
                }else {continue;}
             } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private String toLowerFirstCase(String simpleName) {

        char[] chars = simpleName.toCharArray();
        //大小写字母的ASCII码相差32，而且要小于小写字母的ASCII码
        //在java中,对Char做算法运算,实际上就是对ASCII码做算学运算
        chars[0] += 32;

        return String.valueOf(chars);

    }

    //注入
    private void doAutowired() {

        if(ioc.isEmpty()){ return; }

        for (Map.Entry<String,Object> entry:ioc.entrySet()) {
            //获取到所有的特定的字段
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field : fields){
                if(!field.isAnnotationPresent(GPAutowride.class)){ continue; }
                GPAutowride gpAutowride = field.getAnnotation(GPAutowride.class);
                String beanName = gpAutowride.value().trim();
                if("".equals(beanName)){
                    //获取接口的类型 一会拿key去ioc容器中去取值
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);
                //反射机制,动态给字段赋值
                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }


    }
    //初始化URL 和 Model 一对一对应关系
    private void initHanderMapping() {
        if (ioc.isEmpty()){return;}
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clzz = entry.getValue().getClass();
            if(!clzz.isAnnotationPresent(GPController.class)){continue;}

            //保存类上面的requestMapping
            String baseurl = "";
            if(clzz.isAnnotationPresent(GPController.class)){
               GPRequestMapping gpRequestMapping =  clzz.getAnnotation(GPRequestMapping.class);
                baseurl = gpRequestMapping.value();
            }

            for (Method method : clzz.getMethods()) {
                if (!method.isAnnotationPresent(GPRequestMapping.class)){continue;}
                GPRequestMapping gpRequestMapping =method.getAnnotation(GPRequestMapping.class);
                String url = (baseurl +"/" +gpRequestMapping.value()).replaceAll("/+","/");
                handleMapping.put(url,method);
                System.out.println("Mapped:"+url+","+method);
            }
        }
    }
}
