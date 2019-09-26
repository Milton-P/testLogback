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

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

/**
 * A factory bean for a JCA {@link SecureRandom} generator.
 * <p>
 * This object holds the configurable properties of a secure random generator
 * and uses them to create and load a {@link SecureRandom} instance.
 *
 * @author Carl Harris
 */
public class SecureRandomFactoryBean {

  private String algorithm;
  private String provider;

  /**
   * Creates a new {@link SecureRandom} generator using the receiver's
   * configuration.
   * @return secure random generator instance
   * @throws NoSuchProviderException if the provider name specified by
   *    {@link #setProvider(String)} is not known to the platform
   * @throws NoSuchAlgorithmException if the algorithm name specified by
   *    {@link #setAlgorithm(String)} is not recognized by the specified
   *    provider (or the platform's default provider if the provider isn't
   *    specified)
   */
  public SecureRandom createSecureRandom() throws NoSuchProviderException,
      NoSuchAlgorithmException {
    try {
      return getProvider() != null ?
          SecureRandom.getInstance(getAlgorithm(), getProvider())
          : SecureRandom.getInstance(getAlgorithm());
    }
    catch (NoSuchProviderException ex) {
      throw new NoSuchProviderException("no such secure random provider: "
          + getProvider());
    }
    catch (NoSuchAlgorithmException ex) {
      throw new NoSuchAlgorithmException("no such secure random algorithm: "
          + getAlgorithm());
    }
  }

  /**
   * Gets the secure random generator algorithm name.
   * @return an algorithm name (e.g. {@code SHA1PRNG}); the
   *    {@link SSL#DEFAULT_SECURE_RANDOM_ALGORITHM} is returned if no algorithm has been
   *    specified
   */
  public String getAlgorithm() {
    if (algorithm == null) {
      return SSL.DEFAULT_SECURE_RANDOM_ALGORITHM;
    }
    return algorithm;
  }

  /**
   * Sets the secure random generator algorithm name.
   * @param algorithm an algorithm name, which must be recognized by the
   *    provider specified via {@link #setProvider(String)} or by the
   *    platform's default provider if no provider is specified.
   */
  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  /**
   * Gets the JCA provider name for the secure random generator.
   * @return provider name
   */
  public String getProvider() {
    return provider;
  }

  /**
   * Sets the JCA provider name for the secure random generator.
   * @param provider name of the JCA provider to utilize in creating the
   *    secure random generator
   */
  public void setProvider(String provider) {
    this.provider = provider;
  }

}
