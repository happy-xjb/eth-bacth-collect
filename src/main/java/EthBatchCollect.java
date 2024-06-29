import com.google.common.collect.ImmutableList;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * 小号钱包余额归集回大号
 * @author happyxjb
 * @date 2023/6/5 20:55
 */
public class EthBatchCollect {
    // 定义常量

    // 主钱包地址，用于接收所有转账的ETH
    private static final String MAIN_WALLET_ADDRESS = "0xE7b5C27B7F87B1F8774A8aC09D090433C48802E2";

    // 以太坊节点的RPC URL，用于连接以太坊网络
    private static final String NETWORK_RPC = "https://eth.drpc.org";

    // 以太坊主网的链ID
    private static final Long NETWORK_CHAIN_ID = 1L;

    // 以太坊交易的Etherscan网址前缀，用于查看交易详情
    private static final String NETWORK_SCAN = "https://etherscan.io/tx/";

    public static void main(String[] args) {
        // 初始化Web3j
        Web3j web3j = Web3j.build(new HttpService(NETWORK_RPC));

        // 私钥列表
        List<String> privateKeys = ImmutableList.of("私钥1", "私钥2", "私钥3", "私钥4", "私钥5");

        // 遍历每个私钥
        for (String privateKey : privateKeys) {
            try {
                // 创建凭证
                Credentials smallCred = Credentials.create(privateKey);

                // 获取账户的nonce
                BigInteger finalNonce = getNonceRetryable(web3j, smallCred.getAddress());

                // 获取当前的gas价格
                BigInteger gasPrice = getCurrentGasPriceRetryable(web3j);

                // 计算动态的gasLimit
                BigInteger gasLimit = estimateGasLimit(web3j, smallCred.getAddress(), MAIN_WALLET_ADDRESS, "");

                // 计算最大可转账ETH余额
                BigDecimal amount = getMaxTransferableEthBalance(web3j, smallCred.getAddress(),
                        MAIN_WALLET_ADDRESS, gasPrice, gasLimit);

                BigDecimal amountWei = Convert.toWei(amount, Convert.Unit.ETHER);

                // 创建原始交易
                RawTransaction rawTransaction = RawTransaction.createTransaction(finalNonce, gasPrice,
                        gasLimit, MAIN_WALLET_ADDRESS, amountWei.toBigInteger(), "");

                // 签名并发送交易
                sendTransactionWithRetry(web3j, smallCred, rawTransaction);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 获取nonce并重试
    private static BigInteger getNonceRetryable(Web3j web3j, String address) throws ExecutionException, InterruptedException {
        while (true) {
            try {
                return getNonce(web3j, address);
            } catch (Exception e) {
                System.out.println("获取nonce失败，重新获取...");
                Thread.sleep(10000L);
            }
        }
    }

    // 获取nonce
    private static BigInteger getNonce(Web3j web3j, String address) throws ExecutionException, InterruptedException {
        return web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
                .sendAsync()
                .get()
                .getTransactionCount();
    }

    // 获取当前的gas价格并重试
    private static BigInteger getCurrentGasPriceRetryable(Web3j web3j) throws ExecutionException, InterruptedException {
        while (true) {
            try {
                return getCurrentGasPrice(web3j);
            } catch (Exception e) {
                System.out.println("获取gas价格失败，重新获取...");
                Thread.sleep(1000L);
            }
        }
    }

    // 获取当前的gas价格
    private static BigInteger getCurrentGasPrice(Web3j web3j) throws ExecutionException, InterruptedException {
        return web3j.ethGasPrice()
                .sendAsync()
                .get()
                .getGasPrice();
    }

    // 估算交易的gasLimit
    private static BigInteger estimateGasLimit(Web3j web3j, String from, String to, String data) throws ExecutionException, InterruptedException {
        Transaction transaction = Transaction.createEthCallTransaction(from, to, data);
        EthEstimateGas ethEstimateGas = web3j.ethEstimateGas(transaction)
                .sendAsync()
                .get();
        return ethEstimateGas.getAmountUsed();
    }

    // 计算最大可转账ETH余额
    private static BigDecimal getMaxTransferableEthBalance(Web3j web3j, String from, String to, BigInteger gasPrice, BigInteger gasLimit) throws ExecutionException, InterruptedException {
        EthGetBalance ethGetBalance = web3j.ethGetBalance(from, DefaultBlockParameterName.LATEST)
                .sendAsync()
                .get();
        BigInteger balance = ethGetBalance.getBalance();
        if (balance.equals(BigInteger.ZERO)) {
            return BigDecimal.ZERO;
        }

        BigInteger gasCost = gasLimit.multiply(gasPrice);
        BigDecimal transferableAmount = Convert.fromWei(balance.subtract(gasCost).toString(), Convert.Unit.ETHER);
        return transferableAmount;
    }

    // 签名并发送交易并重试
    private static void sendTransactionWithRetry(Web3j web3j, Credentials credentials, RawTransaction rawTransaction) {
        while (true) {
            try {
                RawTransactionManager transactionManager = new RawTransactionManager(web3j, credentials, NETWORK_CHAIN_ID);
                EthSendTransaction ethSendTransaction = transactionManager.signAndSend(rawTransaction);
                String txHash = ethSendTransaction.getTransactionHash();
                if (txHash == null) {
                    System.out.println("交易发送失败: " + ethSendTransaction.getError().getMessage());
                    break;
                } else {
                    System.out.println("交易发送成功: " + NETWORK_SCAN + txHash);
                }
                break;
            } catch (Exception e) {
                System.out.println("发送交易失败，重新发送...");
            }
        }
    }
}