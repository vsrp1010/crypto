import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class TxHandler {

    private UTXOPool thePool;
    
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.thePool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        return allOutputsInCurrentUtxoPool(tx)
                && allSignaturesValid(tx)
                && noMultipleClaimedUtxo(tx)
                && allOutputValuesNonNegative(tx)
                && outputValuesNotMoreThanInputValues(tx);
    }

    private boolean allOutputsInCurrentUtxoPool(Transaction tx) {
        for(Transaction.Input input: tx.getInputs()) {
            if( ! this.thePool.contains(new UTXO(input.prevTxHash, input.outputIndex))) {
                return false;
            }
        }
        return true;
    }

    private boolean allSignaturesValid(Transaction tx) {
        for(int i=0; i < tx.getInputs().size(); i++) {
            Transaction.Input input = tx.getInput(i);
            Transaction.Output output = thePool.getTxOutput(new UTXO(input.prevTxHash, input.outputIndex));
            if(output != null) {
                if( ! Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean noMultipleClaimedUtxo(Transaction tx) {
        Set<UTXO> currentClaimedUtxo = new HashSet<UTXO>();
        for(int i=0; i < tx.getInputs().size(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO currentUtxo = new UTXO(input.prevTxHash, input.outputIndex);
            // add will only fail if the utxo being added is already present in the set
            if( ! currentClaimedUtxo.add(currentUtxo)) {
                return false;
            }
        }
        return true;
    }

    private boolean allOutputValuesNonNegative(Transaction tx) {
        for(Transaction.Output output: tx.getOutputs()) {
            if(output.value < 0.0)
                return false;
        }
        return true;
    }

    private boolean outputValuesNotMoreThanInputValues(Transaction tx) {
        double outputValue = 0.0;
        for(Transaction.Output output: tx.getOutputs()) {
            outputValue += output.value;
        }
        double inputValue = 0.0;
        for(Transaction.Input input: tx.getInputs()) {
            Transaction.Output output = thePool.getTxOutput(new UTXO(input.prevTxHash, input.outputIndex));
            if(output != null) {
                inputValue += output.value;
            } else {
                return false;
            }
        }
        return inputValue >= outputValue;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        List<Transaction> validTxs = new ArrayList<Transaction>();
        for(Transaction tx : possibleTxs) {
            if(isValidTx(tx)) {
                // remove claimed UTXOs
                for(int i=0; i < tx.getInputs().size(); i++) {
                    Transaction.Input input = tx.getInput(i);
                    UTXO currentUtxo = new UTXO(input.prevTxHash, input.outputIndex);
                    thePool.removeUTXO(currentUtxo);
                }
                for(int i=0; i < tx.getOutputs().size(); i++) {
                    Transaction.Output output = tx.getOutput(i);
                    thePool.addUTXO(new UTXO(tx.getHash(), i), output);
                }
                validTxs.add(tx);
            }
        }
        Transaction[] acceptedTransactions = null;
        if(validTxs.size() > 0) {
            acceptedTransactions = new Transaction[validTxs.size()];
            int index = 0;
            for(Transaction tx: validTxs) {
                acceptedTransactions[index++] = tx;
            }
        }
        return acceptedTransactions;
    }
}
