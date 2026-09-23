# ThingsBoard 缓存一致性示例

这是一个独立的 Java/Maven 示例，用来学习 ThingsBoard 版本化本地缓存、事务提交后的缓存失效和跨节点失效事件传播。

## 演示内容

- 业务数据读取和本地缓存
- 事务内更新业务数据
- 事务提交后发布缓存失效事件
- 多节点本地缓存失效
- 旧版本失效事件保护
- tombstone 防止旧数据回填
- 数据库写入失败时不发布失效事件
- 缓存事务提交和回滚

## 对应 ThingsBoard 源码

- `common/cache/src/main/java/org/thingsboard/server/cache/VersionedCaffeineTbCache.java`
- `common/cache/src/main/java/org/thingsboard/server/cache/CaffeineTbCacheTransaction.java`
- `common/cache/src/main/java/org/thingsboard/server/cache/VersionedTbCache.java`

本项目使用 Caffeine 和内存 Map，分别模拟本地缓存和事务数据库；缓存事件 bus 模拟跨节点事件或队列通知。

## 运行

```bash
cd thingsboard-cache-consistency-demo
mvn clean test
mvn compile exec:java \
  -Dexec.mainClass=org.example.thingsboard.cacheconsistency.CacheConsistencyDemo
```

程序会演示提交后失效、旧事件保护和失败写入不发布失效事件。

## 关键顺序

```text
开启业务事务
    -> 更新业务数据
    -> 提交事务
    -> 发布缓存失效事件
    -> 各节点删除或标记旧缓存
```

失效事件不能在数据库提交前发送，否则数据库回滚后可能产生错误的缓存失效状态。
