/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.client.action.count;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.count.CountRequest;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.support.broadcast.BroadcastOperationThreading;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.action.support.BaseRequestBuilder;
import org.elasticsearch.index.query.QueryBuilder;

/**
 * A count action request builder.
 *
 * @author kimchy (shay.banon)
 */
public class CountRequestBuilder extends BaseRequestBuilder<CountRequest, CountResponse> {

    public CountRequestBuilder(Client client) {
        super(client, new CountRequest());
    }

    /**
     * Sets the indices the count query will run against.
     */
    public CountRequestBuilder setIndices(String... indices) {
        request.indices(indices);
        return this;
    }

    /**
     * The types of documents the query will run against. Defaults to all types.
     */
    public CountRequestBuilder setTypes(String... types) {
        request.types(types);
        return this;
    }

    /**
     * The query parse name to use. If not set, will use the default one.
     */
    public CountRequestBuilder setQueryParserName(String queryParserName) {
        request.queryParserName(queryParserName);
        return this;
    }

    /**
     * The minimum score of the documents to include in the count. Defaults to <tt>-1</tt> which means all
     * documents will be included in the count.
     */
    public CountRequestBuilder setMinScore(float minScore) {
        request.minScore(minScore);
        return this;
    }

    /**
     * A query hint to optionally later be used when routing the request.
     */
    public CountRequestBuilder setQueryHint(String queryHint) {
        request.queryHint(queryHint);
        return this;
    }

    /**
     * The query source to execute.
     *
     * @see org.elasticsearch.index.query.xcontent.QueryBuilders
     */
    public CountRequestBuilder setQuery(QueryBuilder queryBuilder) {
        request.query(queryBuilder);
        return this;
    }

    /**
     * The query source to execute.
     *
     * @see org.elasticsearch.index.query.xcontent.QueryBuilders
     */
    public CountRequestBuilder setQuery(byte[] querySource) {
        request.query(querySource);
        return this;
    }

    /**
     * Controls the operation threading model.
     */
    public CountRequestBuilder setOperationThreading(BroadcastOperationThreading operationThreading) {
        request.operationThreading(operationThreading);
        return this;
    }

    /**
     * Should the listener be called on a separate thread if needed.
     */
    public CountRequestBuilder setListenerThreaded(boolean threadedListener) {
        request.listenerThreaded(threadedListener);
        return this;
    }

    @Override protected void doExecute(ActionListener<CountResponse> listener) {
        client.count(request, listener);
    }
}
