
# EthBatchCollect

## 项目简介

EthBatchCollect 是一个使用 Java 和 Web3j 库编写的程序，用于将多个以太坊（ETH）钱包中的资金批量转移到一个主钱包地址。此工具对于需要管理多个钱包并将资金集中到一个地址的用户非常有用。

## 环境依赖

在运行该程序之前，请确保您的环境中已安装以下依赖：

- Java 8 或更高版本
- Maven（用于构建和管理项目依赖）
- Web3j 库

## 安装和构建

1. 克隆此仓库到您的本地机器：

2. 进入项目目录：
   ```bash
   cd EthBatchCollect
   ```

3. 使用 Maven 构建项目：
   ```bash
   mvn clean install
   ```

## 使用说明

1. 在代码中设置以下常量：
    - `MAIN_WALLET_ADDRESS`: 您的主钱包地址，用于接收所有转账的ETH。
    - `NETWORK_RPC`: 以太坊节点的RPC URL，用于连接以太坊网络。
    - `NETWORK_CHAIN_ID`: 以太坊网络的链ID。1 表示以太坊主网。
    - `NETWORK_SCAN`: 以太坊交易的Etherscan网址前缀，用于查看交易详情。

2. 在 `main` 方法中添加您的私钥列表：
   ```java
   List<String> privateKeys = ImmutableList.of("私钥1", "私钥2", "私钥3", "私钥4", "私钥5");
   ```

3. 运行程序：
   ```bash
   mvn exec:java -Dexec.mainClass="EthBatchCollect"
   ```

## 代码说明

该程序主要包括以下几个部分：

- **常量定义**：包含主钱包地址、以太坊节点的RPC URL、链ID和Etherscan网址前缀。
- **主方法（`main`）**：初始化Web3j对象，遍历私钥列表，获取每个钱包的nonce和当前gas价格，计算最大可转账的ETH余额，创建并发送交易。
- **辅助方法**：
    - `getNonceRetryable` 和 `getNonce`：获取账户的nonce值，带有重试机制。
    - `getCurrentGasPriceRetryable` 和 `getCurrentGasPrice`：获取当前的gas价格，带有重试机制。
    - `estimateGasLimit`：估算交易的gasLimit。
    - `getMaxTransferableEthBalance`：计算账户中扣除gas费用后最大可转账的ETH余额。
    - `sendTransactionWithRetry`：签名并发送交易，带有重试机制以确保交易成功。

## 注意事项

- **安全性**：私钥非常敏感，请确保您的私钥不会泄露。建议在运行此程序前，确保环境是安全的，并且代码中不包含硬编码的私钥。
- **网络连接**：请确保您的网络连接稳定，以便程序能够顺利与以太坊节点进行通信。
- **Gas 费用**：请确保每个钱包中有足够的ETH用于支付转账时的gas费用。
