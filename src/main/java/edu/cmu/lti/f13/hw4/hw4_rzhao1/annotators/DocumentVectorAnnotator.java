package edu.cmu.lti.f13.hw4.hw4_rzhao1.annotators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.TokenNGramTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

import edu.cmu.lti.f13.hw4.hw4_rzhao1.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_rzhao1.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_rzhao1.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {
  
  static TokenizerFactory TOKENIZER_FACTORY = new RegExTokenizerFactory("(-|'|\\d|\\p{L})+");
  
	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}
	/**
	 * 
	 * @param jcas
	 * @param doc
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		String docText = doc.getText();
		ArrayList<String> tokens=new ArrayList<String>();
		ArrayList<Token> finalList=new ArrayList<Token>();
		HashSet<String> tokenSet=new HashSet<String>();
		//TO DO: construct a vector of tokens and update the tokenList in CAS
		 Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(docText.toCharArray(), 0,
             docText.length());
     Iterator<String> tokenList = tokenizer.iterator();
     
     while(tokenList.hasNext()){
       String currentToken=tokenList.next();
       tokens.add(currentToken);
       tokenSet.add(currentToken);
     }
     
     Iterator iterator = tokenSet.iterator(); 
     while(iterator.hasNext()){
       String currentToken=(String)iterator.next();
       int count=0;
       for(int i=0;i<tokens.size();i++){
         if(tokens.get(i).equals(currentToken)){
           count++;
         }
       }
       Token token = new Token(jcas);
       token.setText(currentToken);
       token.setFrequency(count);
   //    System.out.print("["+currentToken+":"+count+"]");
       token.addToIndexes();
       finalList.add(token);
     }
     
     doc.setTokenList(Utils.fromCollectionToFSList(jcas,finalList));
     
	}

}
