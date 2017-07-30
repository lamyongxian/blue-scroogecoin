/**
 * Copyright (c) 2017 Lam YongXian. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package scroogecoin;

import java.util.ArrayList;

public class TxHandler {
	
	private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
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
    	double dblSumOfOutput = 0;
    	double dblSumOfInput = 0;
    	
    	for (Transaction.Output out : tx.getOutputs()) {
    		
    		//(4) check output value is more than zero
    		if(out.value < 0) return false;
    		
    		//Add up all output value in this transaction
    		dblSumOfOutput += out.value;
    		
    	}
    	
    	int signing_index = 0;
    	UTXOPool UniqueUTXOChecked = new UTXOPool();
    	
    	for (Transaction.Input in : tx.getInputs()) {
    		
    		// Get UTXO for current transaction
    		byte[] prevHash = in.prevTxHash;
    		int oIndex = in.outputIndex;
    		UTXO currUTXO = new UTXO(prevHash,oIndex);
    		
    		//(1) check if utxo with prevHash & outputIndex already exist in current UTXO pool
    		boolean bOutputExists = this.utxoPool.contains(currUTXO);
    		if (!bOutputExists) return false; //return false if an output does not exist in pool
    		
    		//Get current output in pool using current UTXO
    		Transaction.Output currOutput = this.utxoPool.getTxOutput(currUTXO);
    		
    		//(2) check input signature is valid with corresponding output public key
    		boolean bValidSignature = Crypto.verifySignature(currOutput.address, tx.getRawDataToSign(signing_index),in.signature);
    		if(!bValidSignature) return false; //return false if an input signature is invalid
    		
    		//(3) if current utxo already checked, is duplicate, return as false
    		if(UniqueUTXOChecked.contains(currUTXO)) {
    			return false;
    		} else {
    			UniqueUTXOChecked.addUTXO(currUTXO, currOutput); //else add into temp utxo pool
    		}
    		
    		//Add up all input value in this transaction
    		dblSumOfInput += currOutput.value;
    		
    		signing_index++;
    	}
    	
    	//(5) return false if sum of output is greater than sum of input
    	if(dblSumOfInput < dblSumOfOutput) return false;
    	
    	return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        
    	ArrayList<Transaction> validTxs = new ArrayList<Transaction>();
    	
    	for (Transaction tx : possibleTxs) {
    		if(this.isValidTx(tx)) {
    			validTxs.add(tx);
    			
    			int currIndex = 0;
    			
    			for (Transaction.Output out : tx.getOutputs()) {
    				UTXO currOutUTXO = new UTXO(tx.getHash(),currIndex);
    				
    				//add unspent utxo into utxo pool
    				this.utxoPool.addUTXO(currOutUTXO, out);
    				
    				currIndex++;
    			}
    			
    			for (Transaction.Input in : tx.getInputs()) {
    				
    	    		// Get UTXO for current transaction
    	    		byte[] prevHash = in.prevTxHash;
    	    		int oIndex = in.outputIndex;
    	    		UTXO currInUTXO = new UTXO(prevHash,oIndex);
    	    		
    	    		//remove spent UTXO from UTXO pool
    	    		this.utxoPool.removeUTXO(currInUTXO);
    				
    			}
    		}
    	}
    	
    	Transaction[] resultTxs = new Transaction[validTxs.size()];
    	return validTxs.toArray(resultTxs);
    }

}
