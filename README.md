# DKU Grade Planner

Java 21 + Swing 的课程成绩与目标分数计算器，COMPSCI 201 三人小组项目第一版。无需 Maven、Gradle、数据库或第三方库。

## 启动

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
- 未保存成绩提示、加载失败保留数据、可重复演示样例。

## 快速体验

启动显示演示数据，不是真实学生成绩。模板权重：Homework10%、Quiz20%、Midterm25%、Final project15%、Final exam30%。

1. 选Homework：HW1为8/10、HW2为90/100、HW3未评分，等权平均85。
2. 点击“编辑类别”，切换“按总点数”，平均约89.09；也可切回等权。
3. 底部选Final exam；双击预测表最后一列，填写Homework90、Quiz85、Final project90。Midterm已结束，使用真实80。
4. 目标85，期末至少85.00；目标95，无法达到，最高预计89.50。
5. 保存XML后打开，真实成绩、权重与顺序恢复；预测值和目标是临时情景，不保存。

新建课程后先添加类别，再添加成绩。反推考试类别须有**一条未评分成绩、正权重、且未结束**。类别全部评分完成后，在“编辑类别”中勾选结束状态，才计入确定贡献。已结束类别要添加未评分条目，先取消结束状态。

## 计算规则

```text
单条百分制成绩 = 得分 / 满分 × 100
等权类别平均 = 已评分条目百分制成绩之和 / 已评分条目数
总点数类别平均 = 已评分条目总得分 / 总满分 × 100
已结束类别贡献 = 类别最终平均 × 权重 / 100
待反推考试要求 = (目标总分 - 其他类别实际或预计贡献) / (该考试权重 / 100)
```

未结束类别的当前平均只代表已有成绩，不直接算为整个类别的最终贡献。预测必须明确输入或点击复制当前平均，并在结果列出假设。已结束类别使用真实成绩；零权重类别不影响预测。

使用BigDecimal、DECIMAL128计算，普通显示保留两位小数；“至少需要”向上取两位。反推给出百分制门槛，实际考试给分粒度可能不同。

权重合计非100%时可编辑保存草稿，但不能反推。全部类别结束且权重100%时，顶部显示最终总分。

## 测试

macOS / Linux：

```bash
bash scripts/test.sh
```

Windows构建后：

```powershell
javac --release 21 -encoding UTF-8 -cp build/classes -d build/test-classes src/test/java/edu/dku/gradeplanner/GradePlannerTest.java
java -Djava.awt.headless=true -cp "build/classes;build/test-classes" edu.dku.gradeplanner.GradePlannerTest
```

覆盖计算、边界和XML存储。[验证记录](docs/validation.md)说明已执行检查与仍需手动验收的范围。

## 代码与团队接手

| 模块 | 文件 | 负责人建议 |
|---|---|---|
| 模型与算法 | Course、GradeCategory、GradeEntry、GradeCalculator | A |
| Swing界面 | MainFrame、App | B |
| 存储、演示数据和测试 | CourseStorage、DemoData、GradePlannerTest | C |

List保存类别与成绩；Map按类别ID查预测；Set校验重复ID和名称。详见[三人计划](docs/team-plan.md)。

## 范围

当前支持一门课的普通加权评分，不处理最低分剔除、额外加分、曲线、考试最低通过线、字母等级或自动PDF解析。每次只反推一项考试，其他未知类别要提供假设。

第一版由Codex辅助实现。团队应理解代码、亲自测试，并根据课程完整AI规则记录使用情况。团队计划中的角色是接手建议，不代表这些代码已由三位成员分别编写。
