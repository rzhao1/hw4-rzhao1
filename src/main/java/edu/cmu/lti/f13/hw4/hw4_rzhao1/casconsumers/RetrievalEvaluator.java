package edu.cmu.lti.f13.hw4.hw4_rzhao1.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f13.hw4.hw4_rzhao1.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_rzhao1.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_rzhao1.utils.Utils;

public class RetrievalEvaluator extends CasConsumer_ImplBase {

  /** query id number **/
  public ArrayList<Integer> qIdList;

  /** query and text relevant values **/
  public ArrayList<Integer> relList;

  public HashSet<Integer> qIdSet;

  // public ArrayList<Document> docList;

  public ArrayList<Integer> MRRList;

  public ArrayList<HashMap<String, Integer>> docList;

  public ArrayList<String> sentenceList;

  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();

    relList = new ArrayList<Integer>();

    docList = new ArrayList<HashMap<String, Integer>>();

    sentenceList = new ArrayList<String>();

    MRRList = new ArrayList<Integer>();

    qIdSet = new HashSet<Integer>();

  }

  /**
   * TODO :: 1. construct the global word dictionary 2. keep the word frequency for each sentence
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {
    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {
      Document doc = (Document) it.next();

      // Make sure that your previous annotators have populated this in CAS
      FSList fsTokenList = doc.getTokenList();
      ArrayList<Token> TokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);
      // ArrayList<Token>tokenList=Utils.fromFSListToCollection(fsTokenList, Token.class);
      qIdList.add(doc.getQueryID());
      relList.add(doc.getRelevanceValue());
      HashMap<String, Integer> tokenList = new HashMap<String, Integer>();
      for (int i = 0; i < TokenList.size(); i++) {
        tokenList.put(TokenList.get(i).getText(), TokenList.get(i).getFrequency());
      }
      docList.add(tokenList);
      sentenceList.add(doc.getText());

      // System.out.println("qID="+doc.getQueryID()+" rel="+doc.getRelevanceValue()+" text="+doc.getText());
      // System.out.println("DOC-ID:"+doc.getQueryID());
      qIdSet.add(doc.getQueryID());
      // Do something useful here
    }
  }

  private double similarityCompute(HashMap<String, Integer> questionList,
          HashMap<String, Integer> answerList) {
    // TODO Auto-generated method stub

    double times = 0.0;
    double questionSquare = 0.0;
    double answerSquare = 0.0;

    for (String questionkey : questionList.keySet()) {
      for (String answerkey : answerList.keySet()) {
        if (questionkey.toLowerCase().equals(answerkey.toLowerCase())) {
          times = times + questionList.get(questionkey) * answerList.get(answerkey);
          break;
        }
      }
    }

    for (String questionkey : questionList.keySet()) {
      questionSquare = questionList.get(questionkey) * questionList.get(questionkey)
              + questionSquare;
    }

    for (String answerkey : answerList.keySet()) {
      answerSquare = answerSquare + answerList.get(answerkey) * answerList.get(answerkey);
    }

    questionSquare = Math.sqrt(questionSquare);
    answerSquare = Math.sqrt(answerSquare);

    return times / (questionSquare * answerSquare);
  }

  /**
   * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2. Compute the MRR metric
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    super.collectionProcessComplete(arg0);
    // System.out.println("docList size is "+docList);
    // TODO :: compute the cosine similarity measure
    for (int i = 0; i < qIdList.size(); i++) {
      if (qIdSet.contains(qIdList.get(i))) {
        qIdSet.remove(qIdList.get(i));
        // New Question and answers
        int currentQID = qIdList.get(i);
        HashMap<String, Integer> question = null;
        double cos_similarity;
        int position;
        int rank = 1;
        HashMap<Integer, Double> similarityMap = new HashMap<Integer, Double>();
        ArrayList<Integer> positionList = new ArrayList<Integer>();
        // Putting question and all the answer candiates to the similarityMap
        for (int j = i; j < qIdList.size(); j++) {
          // The current item is answer candidate
          if (qIdList.get(j) != currentQID) {
            continue;
          }
          if (relList.get(j) == 0 || relList.get(j) == 1) {
            positionList.add(j);
          }

          // The current item is question
          else if (relList.get(j) == 99) {
            question = docList.get(j);

          }

        }

        // Read for calculating the cosine simialrity
        for (Integer key : positionList) {
          double temp = similarityCompute(question, docList.get(key));
          similarityMap.put(key, temp);

        }

        for (Integer key : similarityMap.keySet()) {
          if (relList.get(key) == 1) {
            position = key;
            cos_similarity = similarityMap.get(key);
            for (Integer subkey : similarityMap.keySet()) {
              if (similarityMap.get(subkey) > cos_similarity) {
                rank++;
              }
            }
            System.out.println("Score:" + cos_similarity + "  rank=" + rank + "  rel="
                    + relList.get(position) + "  qid=" + qIdList.get(position) + "  "
                    + sentenceList.get(position));
            break;
          }

        }

        MRRList.add(rank);

      }
    }

    // TODO :: compute the rank of retrieved sentences
    double rankSum = 0;
    for (int i = 0; i < MRRList.size(); i++) {
      rankSum = rankSum + 1 / MRRList.get(i);
    }
    // TODO :: compute the metric:: mean reciprocal rank
    double metric_mrr = rankSum / MRRList.size();
    System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
  }

  /**
   * 
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double cosine_similarity = 0.0;

    // TODO :: compute cosine similarity between two sentences

    return cosine_similarity;
  }

  // /**
  // *
  // * @return mrr
  // */
  // private double compute_mrr() {
  // double metric_mrr = 0.0;
  //
  // // TODO :: compute Mean Reciprocal Rank (MRR) of the text collection
  //
  // return metric_mrr;
  // }

}
