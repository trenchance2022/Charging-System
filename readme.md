# 程序使用说明

## 数据库

1. 下载MySQL8.4
2. 可以按照backend\src\main\resources\application.properties中的参数进行设置，也可自行设置后修改application.properties
3. 创建数据库后按照项目中backend\sql\init.sql中的语句进行建表和初始数据插入
4. 建议使用数据库图形用户界面管理软件

## 后端

1. 在IDEA中将backend文件夹作为项目打开，使用maven加载依赖项
2. 在backend\src\main\resources\application.properties中修改
   1. spring.datasource.url=jdbc:mysql://localhost:端口号/数据库名?useSSL=false
   2. spring.datasource.username=root
   3. spring.datasource.password=密码
3. 启动BackendApplication.java

## 前端

1. 终端进入frontend文件夹下
2. 首次运行输入npm install安装依赖（会出现node_modules文件夹）
3. 输入npm run dev
4. 在浏览器中输入终端中显示的地址