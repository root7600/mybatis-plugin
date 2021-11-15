package com.yan.mybatis.interceptor;


import com.yan.mybatis.Config.GlobalSlowSqlConfigurationProperties;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * @author hairui
 * @date 2021/11/14
 * @des mybatis执行器插件
 */
@Component
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class,
                RowBounds.class, ResultHandler.class})})
public class ExecuteStatInterceptor implements Interceptor {

    private Properties properties;

    private GlobalSlowSqlConfigurationProperties globalSlowSqlConfigurationProperties;
    private static final Logger logger = LoggerFactory.getLogger(ExecuteStatInterceptor.class);

    public ExecuteStatInterceptor(GlobalSlowSqlConfigurationProperties globalSlowSqlConfigurationProperties) {
        this.globalSlowSqlConfigurationProperties = globalSlowSqlConfigurationProperties;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if(!globalSlowSqlConfigurationProperties.getSwitchStatus()){
            return invocation.proceed();
        }
        long start = 0L;
        String sqlId = "";
        BoundSql boundSql = null;
        Configuration configuration = null;
        Object returnValue = null;

        try {
            MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
            sqlId = mappedStatement.getId();
            if(sqlId.contains("History") || sqlId.contains("Tmp")){
                return invocation.proceed();
            }
            Object parameter = null;
            if (invocation.getArgs().length > 1) {
                parameter = invocation.getArgs()[1];
            }
            boundSql = mappedStatement.getBoundSql(parameter);
            configuration = mappedStatement.getConfiguration();
            start = System.currentTimeMillis();
        } catch (Exception e) {
            logger.debug("Mybatis拦截器前置处理异常 原因:", e);
            logger.error("Mybatis拦截器前置处理异常 原因:" + e);
        }

        returnValue = invocation.proceed();

        try {
            long end = System.currentTimeMillis();
            long time = (end - start);
            String sql = getSql(configuration, boundSql, sqlId, time);
            if(globalSlowSqlConfigurationProperties.getTime()!=null&&globalSlowSqlConfigurationProperties.getTime()<=time){
                logger.warn(sql);
            }else {
                logger.info(sql);
            }
        } catch (Exception e) {
            logger.debug("Mybatis拦截器后置处理异常 原因:", e);
            logger.error("Mybatis拦截器后置处理异常 原因:" + e);
        }

        return returnValue;
    }

    public static String getSql(Configuration configuration, BoundSql boundSql, String sqlId, long time) {
        String sql = showSql(configuration, boundSql);
        StringBuilder str = new StringBuilder(100);
        str.append("【sqlId】").append(sqlId);
        str.append("【SQL耗时-").append(time).append("-毫秒】");
        str.append("【SQL】").append(sql);
        //logger.debug(SQLFormatter.format(str.toString()));
        logger.debug(str.toString());
        return str.toString();
    }

    private static String getParameterValue(Object obj) {
        String value = null;
        if (obj instanceof String) {
            value = "'" + obj.toString() + "'";
            value = value.replaceAll("\\\\", "\\\\\\\\");
            value = value.replaceAll("\\$", "\\\\\\$");
        } else if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "'" + formatter.format(obj) + "'";
        } else {
            if (obj != null) {
                value = obj.toString();
            } else {
                value = "";
            }

        }
        return value;
    }

    public static String showSql(Configuration configuration, BoundSql boundSql) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        if (parameterMappings.size() > 0 && parameterObject != null) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                sql = sql.replaceFirst("\\?", getParameterValue(parameterObject));

            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    if (metaObject.hasGetter(propertyName)) {
                        Object obj = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(obj));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(obj));
                    }
                }
            }
        }
        return sql;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    @Override
    public void setProperties(Properties properties0) {
        this.properties = properties0;
    }

}
