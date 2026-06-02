# zrlog-plugin-importer

ZrLog 博客导入插件。上传 Hexo Markdown zip 包后，将包内文章按发布时间写入 ZrLog。

## 功能

- 上传 Hexo Markdown zip 文件
- 解析 Markdown front matter
- 按原发布时间导入文章
- 保留可识别的标题、别名、缩略图等文章字段

## 构建

```shell
export JAVA_HOME=${HOME}/dev/graalvm-jdk-latest
export PATH=${JAVA_HOME}/bin:$PATH
```
