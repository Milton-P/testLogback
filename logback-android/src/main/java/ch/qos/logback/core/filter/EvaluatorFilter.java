/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.core.filter;

import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluator;
import ch.qos.logback.core.spi.FilterReply;

/**
 * The value of the {@link #onMatch} and {@link #onMismatch} attributes is set
 * to {@link FilterReply#NEUTRAL}, so that a badly configured evaluator filter does
 * not disturb the functioning of the filter chain. 
 * 
 * <p>It is expected that one of the two attributes will have its value changed
 * to {@link FilterReply#ACCEPT} or {@link FilterReply#DENY}. That way, it is possible to
 * decide if a given result must be returned after the evaluation either failed
 * or succeeded.
 * 
 * 
 * <p> For more information about filters, please refer to the online manual at
 * http://logback.qos.ch/manual/filters.html
 * 
 * @author Ceki G&uuml;lc&uuml;
 * @author S&eacute;bastien Pennec
 */
public class EvaluatorFilter<E> extends AbstractMatcherFilter<E> {

  EventEvaluator<E> evaluator;

  @Override
  public void start() {
    if (evaluator != null) {
      super.start();
    } else {
      addError("No evaluator set for filter " + this.getName());
    }
  }

  public EventEvaluator<E> getEvaluator() {
    return evaluator;
  }

  public void setEvaluator(EventEvaluator<E> evaluator) {
    this.evaluator = evaluator;
  }

  public FilterReply decide(E event) {
    // let us not throw an exception
    // see also bug #17.
    if (!isStarted() || !evaluator.isStarted()) {
      return FilterReply.NEUTRAL;
    }
    try {
      if (evaluator.evaluate(event)) {
        return onMatch;
      } else {
        return onMismatch;
      }
    } catch (EvaluationException e) {
      addError("Evaluator " + evaluator.getName() + " threw an exception", e);
      return FilterReply.NEUTRAL;
    }
  }

}
