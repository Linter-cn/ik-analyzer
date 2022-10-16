/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 */
package org.wltea.analyzer.dic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 词典管理类,单子模式
 */
public class Dictionary {

    /*
     * 词典单子实例
     */
    private static volatile Dictionary singleton;

    private DictSegment _MainDict;

    private DictSegment _QuantifierDict;

    private DictSegment _StopWords;

    private static final Logger logger = LogManager.getLogger(Dictionary.class);

    private static final String PATH_DIC_MAIN = "main.dic";
    private static final String PATH_DIC_SURNAME = "surname.dic";
    private static final String PATH_DIC_QUANTIFIER = "quantifier.dic";
    private static final String PATH_DIC_SUFFIX = "suffix.dic";
    private static final String PATH_DIC_PREP = "preposition.dic";
    private static final String PATH_DIC_STOP = "stop_word.dic";

    private final static String FILE_NAME = "IKAnalyzer.cfg.xml";
    private final static String EXT_DICT = "ext_dict";
    private final static String EXT_STOP = "ext_stop_words";

    private final Properties props;

    private Dictionary() {
        this.props = new Properties();
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream input;
        logger.info("try load config from {}", FILE_NAME);
        input = classLoader.getResourceAsStream(FILE_NAME);
        if (input != null) {
            try {
                props.loadFromXML(input);
            } catch (IOException e) {
                logger.error("ik-analyzer", e);
            }
        }
    }

    private String getProperty(String key) {
        if (props != null) {
            return props.getProperty(key);
        }
        return null;
    }

    /**
     * 词典初始化 由于IK Analyzer的词典采用Dictionary类的静态方法进行词典初始化
     * 只有当Dictionary类被实际调用时，才会开始载入词典， 这将延长首次分词操作的时间 该方法提供了一个在应用加载阶段就初始化字典的手段
     */
    public static synchronized void initial() {
        if (singleton == null) {
            synchronized (Dictionary.class) {
                if (singleton == null) {
                    singleton = new Dictionary();
                    singleton.loadMainDict();
                    singleton.loadSurnameDict();
                    singleton.loadQuantifierDict();
                    singleton.loadSuffixDict();
                    singleton.loadPrepDict();
                    singleton.loadStopWordDict();
                }
            }
        }
    }

    private void walkFileTree(List<String> files, Path path) {
        if (Files.isRegularFile(path)) {
            files.add(path.toString());
        } else if (Files.isDirectory(path)) {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        files.add(file.toString());
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException e) {
                        logger.error("[Ext Loading] listing files", e);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                logger.error("[Ext Loading] listing files", e);
            }
        } else {
            logger.warn("[Ext Loading] file not found: " + path);
        }
    }

    private void loadDictFile(DictSegment dict, String file, ClassLoader classLoader, boolean critical, String name) {
        try (InputStream is = classLoader == null ? new FileInputStream(file) : classLoader.getResourceAsStream(file)) {
            if (is == null) {
                throw new FileNotFoundException();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8), 512);
            logger.info("ik-analyzer: loaded " + name);
            String word = br.readLine();
            if (word != null) {
                if (word.startsWith("\uFEFF"))
                    word = word.substring(1);
                for (; word != null; word = br.readLine()) {
                    word = word.trim();
                    if (word.isEmpty()) continue;
                    dict.fillSegment(word.toCharArray());
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("ik-analyzer: " + name + " not found", e);
            if (critical) throw new RuntimeException("ik-analyzer: " + name + " not found!!!", e);
        } catch (IOException e) {
            logger.error("ik-analyzer: " + name + " loading failed", e);
        }
    }

    private List<String> getDictionaries(String configKey) {
        String dictCfg = getProperty(configKey);
        List<String> dictFiles = new ArrayList<>(2);
        ClassLoader classLoader = this.getClass().getClassLoader();
        if (dictCfg != null) {
            String[] filePaths = dictCfg.split(";");
            for (String filePath : filePaths) {
                if (filePath != null && !"".equals(filePath.trim())) {
                    filePath = filePath.trim();
                    if (filePath.startsWith("/")) {
                        filePath = filePath.substring(1);
                    }
                    URL url = classLoader.getResource(filePath);
                    if (url == null) {
                        logger.error("ik-analyzer: " + filePath + " not found");
                        continue;
                    }
                    try {
                        Path file = Path.of(url.toURI());
                        walkFileTree(dictFiles, file);
                    } catch (URISyntaxException e) {
                        logger.error("ik-analyzer: " + filePath + " loading failed", e);
                    }
                }
            }
        }
        return dictFiles;
    }

    private List<String> getExtDictionaries() {
        return getDictionaries(EXT_DICT);
    }

    private List<String> getExtStopWordDictionaries() {
        return getDictionaries(EXT_STOP);
    }

    /**
     * 获取词典单子实例
     *
     * @return Dictionary 单例对象
     */
    public static Dictionary getSingleton() {
        if (singleton == null) {
            throw new IllegalStateException("ik dict has not been initialized yet, please call initial method first.");
        }
        return singleton;
    }

    /**
     * 检索匹配主词典
     *
     * @return Hit 匹配结果描述
     */
    public Hit matchInMainDict(char[] charArray, int begin, int length) {
        return singleton._MainDict.match(charArray, begin, length);
    }

    /**
     * 检索匹配量词词典
     *
     * @return Hit 匹配结果描述
     */
    public Hit matchInQuantifierDict(char[] charArray, int begin, int length) {
        return singleton._QuantifierDict.match(charArray, begin, length);
    }

    /**
     * 从已匹配的Hit中直接取出DictSegment，继续向下匹配
     *
     * @return Hit
     */
    public Hit matchWithHit(char[] charArray, int currentIndex, Hit matchedHit) {
        DictSegment ds = matchedHit.getMatchedDictSegment();
        return ds.match(charArray, currentIndex, 1, matchedHit);
    }

    /**
     * 判断是否是停止词
     *
     * @return boolean
     */
    public boolean isStopWord(char[] charArray, int begin, int length) {
        return singleton._StopWords.match(charArray, begin, length).isMatch();
    }

    /**
     * 加载主词典及扩展词典
     */
    private void loadMainDict() {
        // 建立一个主词典实例
        _MainDict = new DictSegment((char) 0);

        // 读取主词典文件
        loadDictFile(_MainDict, Dictionary.PATH_DIC_MAIN, this.getClass().getClassLoader(), false, "Main Dict");

        // 加载扩展词典
        List<String> extDictFiles = getExtDictionaries();
        for (String extDictName : extDictFiles) {
            // 读取扩展词典文件
            logger.info("[Dict Loading] " + extDictName);
            loadDictFile(_MainDict, extDictName, null, false, "Extra Dict");
        }
    }

    /**
     * 加载用户扩展的停止词词典
     */
    private void loadStopWordDict() {
        // 建立主词典实例
        _StopWords = new DictSegment((char) 0);

        // 读取主词典文件
        loadDictFile(_StopWords, Dictionary.PATH_DIC_STOP, this.getClass().getClassLoader(), false, "Main Stopwords");

        // 加载扩展停止词典
        List<String> extStopWordDictFiles = getExtStopWordDictionaries();
        for (String extStopWordDictName : extStopWordDictFiles) {
            logger.info("[Dict Loading] " + extStopWordDictName);
            loadDictFile(_StopWords, extStopWordDictName, null, false, "Extra Stopwords");
        }
    }

    /**
     * 加载量词词典
     */
    private void loadQuantifierDict() {
        // 建立一个量词典实例
        _QuantifierDict = new DictSegment((char) 0);
        // 读取量词词典文件
        loadDictFile(_QuantifierDict, Dictionary.PATH_DIC_QUANTIFIER, this.getClass().getClassLoader(), false, "Quantifier");
    }

    private void loadSurnameDict() {
        DictSegment _SurnameDict = new DictSegment((char) 0);
        loadDictFile(_SurnameDict, Dictionary.PATH_DIC_SURNAME, this.getClass().getClassLoader(), true, "Surname");
    }

    private void loadSuffixDict() {
        DictSegment _SuffixDict = new DictSegment((char) 0);
        loadDictFile(_SuffixDict, Dictionary.PATH_DIC_SUFFIX, this.getClass().getClassLoader(), true, "Suffix");
    }

    private void loadPrepDict() {
        DictSegment _PrepDict = new DictSegment((char) 0);
        loadDictFile(_PrepDict, Dictionary.PATH_DIC_PREP, this.getClass().getClassLoader(), true, "Preposition");
    }

}
