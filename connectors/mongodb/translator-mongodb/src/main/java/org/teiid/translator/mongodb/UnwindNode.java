/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.translator.mongodb;

import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;
import org.teiid.translator.TranslatorException;

import com.mongodb.BasicDBObject;

public class UnwindNode extends ProcessingNode {

    public UnwindNode(MongoDocument document) {
        super(document);
    }

    @Override
    public BasicDBObject getInstruction() throws TranslatorException {
        LogManager.logDetail(LogConstants.CTX_CONNECTOR, "{\"$unwind\": {$"+getDocumentName()+"}}"); //$NON-NLS-1$ //$NON-NLS-2$
        return new BasicDBObject("$unwind", "$"+getDocumentName()); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
