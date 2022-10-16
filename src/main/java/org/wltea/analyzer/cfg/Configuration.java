/**
 *
 */
package org.wltea.analyzer.cfg;

import org.wltea.analyzer.dic.Dictionary;

public class Configuration {

    //是否启用智能分词
    private boolean useSmart;

    //是否启用小写处理
    private boolean enableLowercase;

    public Configuration() {
        this.useSmart = false;
        this.enableLowercase = true;
        Dictionary.initial();
    }

    public void setUseSmart(boolean useSmart) {
        this.useSmart = useSmart;
    }

    public boolean isUseSmart() {
        return useSmart;
    }

    public boolean isEnableLowercase() {
        return enableLowercase;
    }

}
