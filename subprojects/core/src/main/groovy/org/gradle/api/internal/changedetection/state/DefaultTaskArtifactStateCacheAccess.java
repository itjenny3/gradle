/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.changedetection.state;

import org.gradle.api.internal.changedetection.LibrarianThread;
import org.gradle.api.invocation.Gradle;
import org.gradle.cache.CacheRepository;
import org.gradle.cache.PersistentCache;
import org.gradle.cache.PersistentIndexedCache;
import org.gradle.cache.internal.FileLockManager;
import org.gradle.internal.Factory;
import org.gradle.messaging.serialize.Serializer;

public class DefaultTaskArtifactStateCacheAccess implements TaskArtifactStateCacheAccess {
    private LibrarianThread librarian;

    public DefaultTaskArtifactStateCacheAccess(final Gradle gradle, final CacheRepository cacheRepository) {
        boolean parallelExecution = gradle.getStartParameter().getParallelThreadCount() != 0;
        this.librarian = new LibrarianThread(parallelExecution, new Factory<PersistentCache>() {
            public PersistentCache create() {
                return cacheRepository
                        .cache("taskArtifacts")
                        .forObject(gradle)
                        .withDisplayName("task artifact state cache")
                        .withLockMode(FileLockManager.LockMode.Exclusive)
                        .open();
            }
        });
    }

    public <K, V> PersistentIndexedCache<K, V> createCache(final String cacheName, final Class<K> keyType, final Class<V> valueType) {
        return inMemory(librarian.createCache(cacheName, keyType, valueType));
    }

    private <K, V> PersistentIndexedCache<K, V> inMemory(PersistentIndexedCache delegate) {
        return new InMemoryDelegatingCache(delegate);
    }

    public <K, V> PersistentIndexedCache<K, V> createCache(final String cacheName, final Class<K> keyType, final Class<V> valueType, final Serializer<V> valueSerializer) {
        return inMemory(librarian.createCache(cacheName, keyType, valueSerializer));
    }

    public void stop() {
        librarian.stop();
    }

    public void start() {
        librarian.start();
    }

    public void useCache(Runnable runnable) {
        librarian.useCache(runnable);
    }

    public void longRunningOperation(Runnable runnable) {
        librarian.longRunningOperation(runnable);
    }

    public <T> T useCache(String operationDisplayName, Factory<? extends T> action) {
        return (T) librarian.useCache(operationDisplayName, action);
    }
}