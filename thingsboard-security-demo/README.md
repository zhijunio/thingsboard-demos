# ThingsBoard Security 安全示例

这是一个独立的 Java/Maven 示例，用来学习 ThingsBoard 基于 Spring Security 的认证、MFA、OAuth2/OIDC 登录、JWT 和租户权限校验。

## 演示内容

- Spring Boot 3.5.15
- Spring Security `SecurityFilterChain`
- `AuthenticationProvider`
- `SecurityContext`
- Bearer JWT filter
- Spring `@EnableMethodSecurity` 和 `@PreAuthorize`
- JJWT HS512 access token
- 本地用户名密码认证
- PBKDF2-HMAC-SHA256 密码派生
- `PRE_VERIFICATION_TOKEN`
- TOTP MFA 和 `otpauth://` URL
- Spring OAuth2 Client
- OAuth2 authorization endpoint
- OAuth2 `state`、`nonce` 和 authorization code
- OAuth2 登录成功后换发平台 JWT
- `SYS_ADMIN`、`TENANT_ADMIN`、`CUSTOMER_USER` 和租户边界

## 对应 ThingsBoard 源码

- `application/src/main/java/org/thingsboard/server/config/ThingsboardSecurityConfiguration.java`
- `application/src/main/java/org/thingsboard/server/service/security/model/SecurityUser.java`
- `application/src/main/java/org/thingsboard/server/service/security/model/token/JwtTokenFactory.java`
- `application/src/main/java/org/thingsboard/server/service/security/auth/rest/RestAuthenticationProvider.java`
- `application/src/main/java/org/thingsboard/server/service/security/auth/mfa/DefaultTwoFactorAuthService.java`
- `application/src/main/java/org/thingsboard/server/service/security/auth/mfa/provider/impl/TotpTwoFaProvider.java`
- `application/src/main/java/org/thingsboard/server/service/security/auth/oauth2/Oauth2AuthenticationSuccessHandler.java`
- `application/src/main/java/org/thingsboard/server/service/security/auth/oauth2/BasicOAuth2ClientMapper.java`
- `application/src/main/java/org/thingsboard/server/service/security/permission/DefaultAccessControlService.java`

## 本地认证和 MFA

```text
POST /api/auth/login
    -> Spring AuthenticationManager
    -> LocalAuthenticationProvider
    -> PRE_VERIFICATION_TOKEN
    -> POST /api/auth/mfa
    -> TOTP 校验
    -> Access JWT
```

演示用户：

```text
username: tenant@example.com
password: tenant-password
TOTP secret: JBSWY3DPEHPK3PXP
```

## Spring Security 请求链

```text
HTTP Request
    -> SecurityFilterChain
    -> JwtAuthenticationFilter
    -> SecurityContext
    -> @PreAuthorize
    -> AccessControlService
    -> Controller
```

API 未认证时返回 `401`；跨租户访问返回 `403`。

## OAuth2

Spring Security 注册了：

```text
GET /oauth2/authorization/demo-provider
```

示例 provider 地址是 `https://idp.example`，只用于演示 OAuth2 client 配置和 redirect 入口，不连接真实第三方身份提供商。无外部 IdP 时，可以使用测试中的 `InMemoryOAuth2Provider` 完成可重复的 authorization code 演示。

## 运行

运行纯 Java 流程：

```bash
cd thingsboard-security-demo
mvn clean test
mvn compile exec:java \
  -Dexec.mainClass=org.example.thingsboard.security.SecurityDemo
```

运行 Spring Security 应用：

```bash
mvn compile exec:java \
  -Dexec.mainClass=org.example.thingsboard.security.SpringSecurityDemoApplication
```

登录请求示例：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"tenant@example.com","password":"tenant-password"}'
```

## 设计边界

用户、MFA 配置和 OAuth2 provider 使用内存实现；示例不包含数据库、真实 OAuth2 IdP、密钥管理、限流和完整审计系统。代码用于学习 ThingsBoard 的 Spring Security 调用方向，不作为生产安全组件。
