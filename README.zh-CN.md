# DKU Grade Planner

[English README](README.md) | **中文版**

使用 Java 21 + Swing 的开源课程成绩与目标分数计算器，正在准备首个公开发行版 **v1.0.0**。无需 Maven、Gradle、数据库或第三方库。

## 启动

发布后可从 [GitHub Releases](https://github.com/ClarkzzZz/dku-grade-planner/releases) 下载 JAR，用 `java -jar dku-grade-planner.jar` 启动。需要自行安装 Java 21 或更新版本；安装包不包含 Java。以下为源码构建方式。

配置 **JDK 21**，确认 `java -version` 和 `javac -version` 均可运行。在仓库根目录执行：

macOS / Linux：

```bash
bash scripts/run.sh
```

Windows PowerShell：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run.ps1
```

构建使用 `javac --release 21`。编译后也可直接运行：

```bash
java -jar build/dku-grade-planner.jar
```

运行需要Java 21或更新版本及图形桌面。`build/`不提交到Git，克隆后重新构建。

IDE中把SDK设置为21，将`src/main/java`标为源码目录，运行`edu.dku.gradeplanner.App`。

## 已实现

- 考核类别、权重和多次成绩的添加、编辑、删除。
- Homework、Quiz逐次录入得分与满分，每类最多1000条。
- 每条等权平均与总点数汇总；空成绩不计入，零分计入。
- 类别当前平均分、已结束类别贡献和已结束类别加权平均。
- 输入目标总分和其他类别的预计最终成绩，反推一项未知考试。
- 显示目标是否可达、考试0/100分时预计总分和计算假设。
- 权重、得分、类别结束状态校验，本地XML保存加载。
- 未保存成绩提示、加载失败保留数据。

## 开始使用

顶部固定显示 **DKU Grade Planner**，下方单独显示课程名称。启动时课程为 **Untitled Course**，没有预置类别或成绩，目标分数也为空。

1. 点击 **Course Name** 设置课程名称。
2. 点击 **Add Category**，按 syllabus 添加考核类别、权重和汇总方式。反推时权重必须合计100%。
3. 选择类别，点击 **Add Entry** 逐次添加 homework、quiz 或考试成绩，填写得分与满分；未评分留空。
4. 类别内全部评分完成后，在 **Edit Category** 中标记结束，实际类别平均才计入确定贡献。
5. 选择一项未结束、正权重、且仅含一条未评分成绩的考试，填写目标分数和其他未结束类别的预计最终平均，点击 **Calculate**。
6. 用 **Save** 保存本地课程，或用 **Open** 打开已有文件。预测值和目标是临时情景，不写入成绩文件。

已结束类别要添加未评分条目，需先在 **Edit Category** 中重新开放。**New** 新建空白课程，并在已有未保存修改时提示保存。应用不附带演示课程文件或预置成绩。

## 计算规则

```text
单条百分制成绩 = 得分 / 满分 × 100
等权类别平均 = 已评分条目百分制成绩之和 / 已评分条目数
总点数类别平均 = 已评分条目总得分 / 总满分 × 100
已结束类别贡献 = 类别最终平均 × 权重 / 100
待反推考试要求 = (目标总分 - 其他类别实际或预计贡献) / (该考试权重 / 100)
```

未结束类别的当前平均只代表已有成绩，不直接算为整个类别的最终贡献。预测必须明确输入或点击 Copy Current Averages，并在结果列出假设。已结束类别使用真实成绩；零权重类别不影响预测。

使用BigDecimal、DECIMAL128计算，普通显示保留两位小数；“至少需要”向上取两位。反推给出百分制门槛，实际考试给分粒度可能不同。

权重合计非100%时可编辑保存草稿，但不能反推。全部类别结束且权重100%时，顶部显示最终总分。

## 测试

macOS / Linux：

```bash
bash scripts/test.sh
```

Windows PowerShell：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test.ps1
```

覆盖计算、边界、语言设置和 XML 存储。[验证说明](docs/validation.md)提供复现命令和手动验收清单。GitHub Actions 配置了 Linux、macOS、Windows 上的 Java 21 / 25 检查；配置完成不代表已在这些环境验证通过。

## 架构与参与贡献

项目分为不可变数据模型（Course、GradeCategory、GradeEntry）、计算服务（GradeCalculator）、XML 存储（CourseStorage）和 Swing 界面（App、MainFrame），详见[架构说明](docs/architecture.md)。

欢迎任何人通过 Issue 和 Pull Request 参与修复、文档、测试和功能开发，不再按三人小组分配模块。请先阅读[贡献指南](CONTRIBUTING.md)。Issue 可以使用中文或英文，请勿上传真实的个人成绩文件。

参见[更新日志](CHANGELOG.md)、[首版发布说明](docs/releases/v1.0.0.md)和[发布清单](docs/releasing.md)。v0.1–v0.3 为课堂原型迭代，v1.0.0 是首个公开发行版本的基线。

## 许可证

采用 [MIT 许可证](LICENSE)。项目起源于课程作业，现面向社区协作。实现过程中使用了 Codex 辅助，贡献者需自行审阅和验证提交的改动。

## 范围

当前支持一门课的普通加权评分，不处理最低分剔除、额外加分、曲线、考试最低通过线、字母等级或自动PDF解析。每次只反推一项考试，其他未知类别要提供假设。
