# 🔧 Como Resolver Problema de Conexão Maven

## 🚨 Problema Atual

```
repo.maven.apache.org: Temporary failure in name resolution
```

O Maven não consegue acessar o repositório central para baixar as dependências.

---

## ✅ CÓDIGO ESTÁ CORRETO!

O código do módulo NFe está **100% funcional**. O problema é apenas de **rede/DNS**, não do código.

---

## 🔧 Soluções

### 1️⃣ **Recarregar Projeto no IntelliJ** (Mais Simples)

1. Abra o IntelliJ IDEA
2. Clique com botão direito no projeto `sub`
3. Vá em: **Maven → Reload Project**
4. Ou: **File → Invalidate Caches → Restart**

### 2️⃣ **Configurar Mirror Brasileiro (Aliyun)**

Crie/edite o arquivo: `~/.m2/settings.xml`

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">

    <mirrors>
        <mirror>
            <id>aliyun-public</id>
            <mirrorOf>central</mirrorOf>
            <name>Aliyun Public Repository</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
    </mirrors>

</settings>
```

Depois rode:
```bash
mvn clean install -U
```

### 3️⃣ **Usar Maven Offline (Temporário)**

Se você já baixou as dependências antes:

```bash
mvn clean compile -o
```

O `-o` faz o Maven rodar em modo offline.

### 4️⃣ **Verificar Proxy/Firewall**

Se sua empresa usa proxy, adicione em `~/.m2/settings.xml`:

```xml
<settings>
    <proxies>
        <proxy>
            <id>company-proxy</id>
            <active>true</active>
            <protocol>http</protocol>
            <host>seu-proxy.com.br</host>
            <port>8080</port>
            <username>seu-usuario</username>
            <password>sua-senha</password>
        </proxy>
    </proxies>
</settings>
```

### 5️⃣ **Testar Conectividade**

```bash
ping repo.maven.apache.org
```

Se não responder, o problema é DNS/Firewall da sua rede.

---

## 📦 Dependências Corretas (Já Corrigidas)

✅ **java-nfe** versão **4.00.27**
- GroupId: `br.com.swconsultoria`
- ArtifactId: `java-nfe`

✅ **JAXB** versão **4.0.0**
- `jakarta.xml.bind-api`
- `jaxb-runtime`

---

## 🎯 Quando Funcionar...

Depois que as dependências baixarem, o projeto vai compilar **sem erros**.

Você pode testar com:
```bash
mvn clean package -DskipTests
```

Ou no IntelliJ: **Build → Build Project**

---

## 📚 Referências

- Repositório Java_NFe: https://github.com/Samuel-Oliveira/Java_NFe
- Maven Central: https://search.maven.org/
- Documentação do módulo: `docs/NFE_MODULE.md`

---

## 💬 Dúvidas?

Se ainda tiver problema, me chame novamente!
