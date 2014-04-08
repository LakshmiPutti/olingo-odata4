/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.client.core.communication.request.invoke.v4;

import java.net.URI;
import java.util.Map;
import org.apache.olingo.client.api.v4.ODataClient;
import org.apache.olingo.client.api.communication.request.invoke.ODataInvokeRequest;
import org.apache.olingo.client.api.communication.request.invoke.ODataNoContent;
import org.apache.olingo.client.api.communication.request.invoke.v4.InvokeRequestFactory;
import org.apache.olingo.client.api.http.HttpMethod;
import org.apache.olingo.commons.api.domain.ODataInvokeResult;
import org.apache.olingo.commons.api.domain.ODataValue;
import org.apache.olingo.client.core.communication.request.invoke.AbstractInvokeRequestFactory;
import org.apache.olingo.commons.api.domain.v4.ODataEntity;
import org.apache.olingo.commons.api.domain.v4.ODataEntitySet;
import org.apache.olingo.commons.api.domain.v4.ODataProperty;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.EdmOperation;
import org.apache.olingo.commons.api.edm.EdmReturnType;
import org.apache.olingo.commons.api.edm.constants.EdmTypeKind;

public class InvokeRequestFactoryImpl extends AbstractInvokeRequestFactory implements InvokeRequestFactory {

  private static final long serialVersionUID = 8452737360003104372L;

  public InvokeRequestFactoryImpl(final ODataClient client) {
    super(client);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <RES extends ODataInvokeResult> ODataInvokeRequest<RES> getInvokeRequest(
          final URI uri, final EdmOperation operation, final Map<String, ODataValue> parameters) {

    final HttpMethod method = operation instanceof EdmAction
            ? HttpMethod.POST
            : HttpMethod.GET;
    final EdmReturnType returnType = operation.getReturnType();

    ODataInvokeRequest<RES> request;
    if (returnType == null) {
      request = (ODataInvokeRequest<RES>) new ODataInvokeRequestImpl<ODataNoContent>(
              client, ODataNoContent.class, method, uri);
    } else {
      if (returnType.isCollection() && returnType.getType().getKind() == EdmTypeKind.ENTITY) {
        request = (ODataInvokeRequest<RES>) new ODataInvokeRequestImpl<ODataEntitySet>(
                client, ODataEntitySet.class, method, uri);
      } else if (!returnType.isCollection() && returnType.getType().getKind() == EdmTypeKind.ENTITY) {
        request = (ODataInvokeRequest<RES>) new ODataInvokeRequestImpl<ODataEntity>(
                client, ODataEntity.class, method, uri);
      } else {
        request = (ODataInvokeRequest<RES>) new ODataInvokeRequestImpl<ODataProperty>(
                client, ODataProperty.class, method, uri);
      }
    }
    if (parameters != null) {
      request.setParameters(parameters);
    }

    return request;
  }
}
