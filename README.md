# Intelligent-Charging-System

## 项目介绍

智能充电系统是一个基于Docker的微服务架构项目，包含前端界面、后端服务和MySQL数据库，用于管理和监控充电设施。

## 系统架构

- **前端**：Vue.js (端口: 80)
- **后端**：Spring Boot (端口: 8080)
- **数据库**：MySQL 8.4.4 (端口: 3306)

## 使用说明

### 环境要求

- Docker
- Docker Compose

### 启动系统

```bash
# 启动所有服务
docker compose up -d

# 查看运行状态
docker compose ps
```

### 访问系统

- **前端界面**：http://localhost

### 使用DBeaver连接数据库

1. 创建MySQL连接
   - 主机：`localhost`
   - 端口：`3306`
   - 数据库：`intelligent_charging_system`
   - 用户名：`root`
   - 密码：`123456`

### 停止系统

```bash
# 停止服务
docker compose down

# 停止服务并删除数据卷
docker compose down -v
```

## 注意事项

- 确保端口80、8080和3306未被其他应用占用
- 系统数据存储在Docker卷中，重启容器不会丢失数据
- 数据库结构在首次启动时通过/backend/db/init.sql自动创建
