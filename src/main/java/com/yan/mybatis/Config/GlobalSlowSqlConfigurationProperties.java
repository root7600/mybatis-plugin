package com.yan.mybatis.Config;

import com.yan.mybatis.common.Properties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author hairui
 * @date 2021/11/14
 * @des
 */
@ConfigurationProperties(Properties.propertiesKey)
public class GlobalSlowSqlConfigurationProperties {

    private Boolean switchStatus;
    private Integer time;

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public Boolean getSwitchStatus() {
        return switchStatus;
    }

    public void setSwitchStatus(Boolean switchStatus) {
        this.switchStatus = switchStatus;
    }
}
