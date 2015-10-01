import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import type.InputDocument;
import type.Passage;
import type.Question;

public class PassageAnnotator extends JCasAnnotator_ImplBase {
  private Pattern mPassagePattern = Pattern.compile("(\\d{4}) ([A-Z0-9]+\\.\\d{4}) (-?[12]) (.*)");

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    System.out.println(">> Passage Annotator Processing");
    // get document text from the CAS
    String docText = aJCas.getDocumentText();

    // search for all the questions in the text
    ArrayList<Passage> passageArray = new ArrayList<Passage>();
    Matcher matcher = mPassagePattern.matcher(docText);
    int pos = 0;
    while (matcher.find(pos)) {
      // found one - create annotation
      Passage annotation = new Passage(aJCas);
      annotation.setBegin(matcher.start());
      annotation.setEnd(matcher.end());
      annotation.setQuestionId(matcher.group(1));
      annotation.setSourceDocId(matcher.group(2));
      annotation.setLabel(!(matcher.group(3).compareTo("-1") == 0));
      annotation.setText(matcher.group(4));
      annotation.addToIndexes();
      pos = matcher.end();
      passageArray.add(annotation);
      // System.out.printf("Added P: %s-%s - %s\n", matcher.group(1), matcher.group(2),
      // matcher.group(4));
    }
    // put passages into their corresponding question
    FSIndex questionIndex = aJCas.getAnnotationIndex(Question.type);
    Iterator questionIterator = questionIndex.iterator();
    while (questionIterator.hasNext()) {
      Question questionAnnot = (Question) questionIterator.next();
      ArrayList<Passage> passagesForThisQuestion = new ArrayList<Passage>();
      for (int j = 0; j < passageArray.size(); j++) {
        if (passageArray.get(j).getQuestionId().equals(questionAnnot.getId())) {
          passagesForThisQuestion.add(passageArray.get(j));
        }
      }
      FSArray passages = new FSArray(aJCas, passagesForThisQuestion.size());
      for (int k = 0; k < passagesForThisQuestion.size(); k++) {
        passages.set(k, passagesForThisQuestion.get(k));
      }
      questionAnnot.setPassages(passages);
    }
  }

}
