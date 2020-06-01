# easy-tx
一款简单易用的无侵入,无需依赖数据库和服务协调者的分布式事务中间件(大量借鉴Seata以及LCN,在此特别感谢)

原理介绍:

wait conn 等待发起方获取全局事务状态后,发送全局事务状态到mq,所有参与方订阅这个主题,收到广播后,把一阶段wait的conn进行一个提交/回滚.

使用说明:需要参与全局事务跟发起全局事务的项目都要加上本项目依赖.

发起方需在发起接口上加入@GlobalTransaction 以及@Transactional 注解.
强依赖spring本地事务,协调所有参与者的业务状态,来做本地事务的统一提交/回滚

本项目需要rocketmq支持,后续考虑支持其他的消息中间件.

本项目仅支持spring cloud的xid传递,dubbo后续会集成.

[使用示例](https://github.com/a364176773/spring-cloud-demo-easy-tx )

```yaml
easy:
  tx:
    enable: true
    rocket:
      name-server: 127.0.0.1:9876
      group: test-group
      topic: test
```

引入依赖:

```
            <dependency>
                <groupId>icu.funkye</groupId>
                <artifactId>easy-tx</artifactId>
                <version>0.1</version>
            </dependency>
```

