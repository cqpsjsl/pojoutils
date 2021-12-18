# pojoConverter
 停止更新,请使用fastbean
#### 介绍
JavaBean之间的转换，DAO->Vo的转换。DAO的字段多，Vo的字段少。需要把DAO里的部分数据赋值给Vo

##### 注意

转换之间参数类型需要一致 String name -----> String userName

#### API

| 返回值 | 方法名                                                       | 说明 |
| ------ | ------------------------------------------------------------ | ---- |
|        | converter(T fromObject, V destObject);                       |      |
|        | converterByOrder(T fromObject, V destObject);                |      |
|        | List<V> listConverter(List<T> formList, List<V> destList, Class<?> destClazz); |      |
|        | setName(String formName, String destName)                    |      |
|        | setIgnoreName(String ignoreName)                             |      |



#### 使用说明

`UserDao` 

```java
/**
* 从数据库查询出来的Bean
**/
@Data
public class UserDao{
  Integer id;
  String name;
  String password;
  String nickname;
}

```

`UserVo`

```java
/**
* 前端需要的
**/
@Data
public class UserVo{
  Integer userId;
  String username;
  String nickname;
}
```

`id`需要赋值给`userId`,`name`需要赋值给`username`,并且`password`字段UserVo是不需要的，如果是几十个字段。get、set很浪费时间。

单个bean

```java
UserDao user = new UserDao(); 
// 假设user是从数据库查询出来的
UserVo userVo = new UserVo();
// 需要将UserDao 中的成员变量赋值给UserVo中，并且舍弃多余的字段

Converter converter = PojoUtils.converter();
converter.setName("id","userId").setName("name","username"); //  名称不一样可在调用converter方法之前调用此方法
converter.converter(user,userVo);
// userVo 赋值完毕
```

List<User>类型

```java
List<UserDao> users = new ArrayList<>();
// 假设这是从数据库查询出来的多条数据
ArrayList<UserVo> userVos = new ArrayList<>();

Converter converter = PojoUtils.converter();
converter.setName("id","userId").setName("name","username"); //  名称不一样可在调用converter方法之前调用此方法
converter.listConverter(users,userVos, UserVo.class);
```

`clear()`

```java
// 清空setName的缓存区
converter.clear();
```



