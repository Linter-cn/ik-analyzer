## IK Analyzer

IK 分词工具库，基于 medcl/elasticsearch-analysis-ik 修改，移除 Elasticsearch 依赖，配置文件和词库文件改为从 resources
目录读取，方便普通 Java 程序调用。

适配 Hutool 封装的
API（[TokenizerUtil](https://hutool.cn/docs/?from_wecom=1#/extra/%E4%B8%AD%E6%96%87%E5%88%86%E8%AF%8D/%E4%B8%AD%E6%96%87%E5%88%86%E8%AF%8D%E5%B0%81%E8%A3%85-TokenizerUtil)
）。

```xml

<dependency>
  <groupId>io.github.linter-cn</groupId>
    <artifactId>ik-analyzer</artifactId>
  <version>1.0</version>
</dependency>
```

### 常见问题

1.如何配置扩展词库？

resources 目录下创建名为 IKAnalyzer.cfg.xml 的配置文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
    <comment>IK Analyzer 扩展配置</comment>
    <!--用户可以在这里配置自己的扩展字典 -->
    <entry key="ext_dict">/data/ext_dic.txt</entry>
    <!--用户可以在这里配置自己的扩展停止词字典-->
    <entry key="ext_stop_words">/data/ext_stop_words.txt</entry>
    <!--可以配置多个路径，使用英文分号分隔，路径可以是文本文件或文件夹-->
</properties>
```

配置的扩展词库文件需要放在 resources 目录下

2.扩展词库为什么没有生效？

请确保你的扩展词典的文本格式为 UTF8 编码
