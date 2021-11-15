package com.yan.mybatis.Config;

import com.yan.mybatis.interceptor.ExecuteStatInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import javax.annotation.PostConstruct;
import java.util.Iterator;
import java.util.List;

/**
 * @author hairui
 * @date 2021/11/14
 * @des
 */
@EnableConfigurationProperties(GlobalSlowSqlConfigurationProperties.class)
public class MybatisSlowSqlPluginConfigure {

    @Autowired
    private GlobalSlowSqlConfigurationProperties globalSlowSqlConfigurationProperties;

    @Autowired
    private List<SqlSessionFactory> sqlSessionFactoryList;

    @PostConstruct
    public void addMybatisPlugin(){
        if(!sqlSessionFactoryList.isEmpty()){
            ExecuteStatInterceptor executeStatInterceptor = new ExecuteStatInterceptor(globalSlowSqlConfigurationProperties);
            Iterator<SqlSessionFactory> iterator = sqlSessionFactoryList.iterator();
            while(iterator.hasNext()){
                SqlSessionFactory next = iterator.next();
                next.getConfiguration().addInterceptor(executeStatInterceptor);
            }
        }
   }
}
