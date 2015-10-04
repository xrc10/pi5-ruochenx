
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import type.Evaluation;
import type.Passage;
import type.Performance;
import type.Question;
import type.Score;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class EvaluationAnnotator extends JCasAnnotator_ImplBase {
  public void process(JCas aJCas) {
    Evaluation evaluationAnnot = new Evaluation(aJCas);
    evaluationAnnot.setComponentId(this.getClass().getName());
    evaluationAnnot.setBegin(0);
    evaluationAnnot.setEnd(aJCas.getDocumentText().length());
    evaluationAnnot.setScore(1.0f);
    FSIndex scoreIndex = aJCas.getAnnotationIndex(Score.type);
    Iterator scoreIterator = scoreIndex.iterator();
    while (scoreIterator.hasNext()) {
      Score score = (Score) scoreIterator.next();
      // rank questions according to Id
      FSArray questions = score.getQuestions();
      FSArray evaluatedQuestions = new FSArray(aJCas, questions.size());
      ArrayList<Question> questionsArray = new ArrayList<Question>(questions.size());
      for (int i = 0; i < questions.size(); i++) {
        questionsArray.add((Question) questions.get(i));
      }
      Collections.sort(questionsArray);
      // iterate each passage over questions
      double pAt1 = 0;
      double pAt5 = 0;
      double mrr = 0;
      double map = 0;
      for (int i = 0; i < questionsArray.size(); i++) {
        Question questionAnnot = questionsArray.get(i);
        // rank passages according to score
        FSArray passages = questionAnnot.getPassages();
        ArrayList<Passage> passageArray = new ArrayList<Passage>(passages.size());
        for (int j = 0; j < passages.size(); j++) {
          passageArray.add((Passage) passages.get(j));
        }
        Collections.sort(passageArray);
        Performance performanceAnnot = new Performance(aJCas);
        performanceAnnot.setPAt1(precisionAtN(passageArray, 1));
        performanceAnnot.setPAt5(precisionAtN(passageArray, 5));
        performanceAnnot.setMap(averagePrecision(passageArray));
        performanceAnnot.setMmr(reciprocalRanking(passageArray));
        pAt1 += precisionAtN(passageArray, 1);
        pAt5 += precisionAtN(passageArray, 5);
        mrr += reciprocalRanking(passageArray);
        map += averagePrecision(passageArray);
        questionAnnot.setPerformance(performanceAnnot);
        evaluatedQuestions.set(i, questionAnnot);
      }
      evaluationAnnot.setQuestions(evaluatedQuestions);
      evaluationAnnot.addToIndexes();
      System.out.printf("pAt1:%f\tpAt5:%f\tmrr:%f\tmap:%f\n",
              pAt1/questionsArray.size(),
              pAt5/questionsArray.size(), 
              mrr/questionsArray.size(), 
              map/questionsArray.size());
    }
  }

  public double averagePrecision(ArrayList<Passage> passageArray) {
    double sumOfPrecision = 0;
    int numberOfHits = 0;
    for (int i = 0; i < passageArray.size(); i++) {
      if (passageArray.get(i).getLabel()) {
        sumOfPrecision += precisionAtN(passageArray, i + 1);
        numberOfHits++;
      }
    }
    if (numberOfHits == 0) {
      return 0;
    } else {
      return sumOfPrecision / numberOfHits;
    }
  }

  public double reciprocalRanking(ArrayList<Passage> passageArray) {
    for (int i = 0; i < passageArray.size(); i++) {
      if (passageArray.get(i).getLabel()) {
        return (double) 1 / (i + 1);
      }
    }
    return 0;
  }

  public double precisionAtN(ArrayList<Passage> passageArray, int n) {
    double numberOfHits = 0;
    for (int i = 0; i < n; i++) {
      if (passageArray.get(i).getLabel()) {
        numberOfHits++;
      }
    }
    return numberOfHits / n;
  }
}
