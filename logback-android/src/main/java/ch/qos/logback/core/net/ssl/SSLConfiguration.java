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
package ch.qos.logback.core.net.ssl;

import javax.net.ssl.SSLContext;

/**
 * A configuration for an {@link SSLContext}.
 * <p>
 *
 * @author Carl Harris
 */
public class SSLConfiguration extends SSLContextFactoryBean {

  private SSLParametersConfiguration parameters;

  /**
   * Gets the SSL parameters configuration.
   * @return parameters configuration; if no parameters object was
   *    configured, a default parameters object is returned
   */
  public SSLParametersConfiguration getParameters() {
    if (parameters == null) {
      parameters = new SSLParametersConfiguration();
    }
    return parameters;
  }

  /**
   * Sets the SSL parameters configuration.
   * @param parameters the parameters configuration to set
   */
  public void setParameters(SSLParametersConfiguration parameters) {
    this.parameters = parameters;
  }

}
