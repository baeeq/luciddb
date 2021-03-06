/*
// Licensed to DynamoBI Corporation (DynamoBI) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  DynamoBI licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at

//   http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
*/
package net.sf.firewater;

import java.util.*;
import java.io.*;

import javax.jmi.reflect.*;

import org.eigenbase.resgen.*;
import org.eigenbase.resource.*;
import org.eigenbase.util.*;

import net.sf.farrago.catalog.*;
import net.sf.farrago.db.*;
import net.sf.farrago.session.*;
import net.sf.farrago.defimpl.*;
import net.sf.farrago.ddl.*;
import net.sf.farrago.parser.*;
import net.sf.farrago.fwm.*;
import net.sf.farrago.fwm.distributed.*;
import net.sf.farrago.cwm.relational.enumerations.*;
import net.sf.farrago.fem.med.*;

import org.luciddb.session.*;
import org.luciddb.jdbc.*;

import net.sf.firewater.resource.*;
import net.sf.firewater.parserimpl.*;

/**
 * FirewaterSessionFactory implements the {@link
 * FarragoSessionSessionFactory} interface by plugging in
 * Firewater distributed SQL processing behavior.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public class FirewaterSessionFactory
    extends LucidDbSessionFactory
    implements FarragoSessionModelExtensionFactory
{
    public static final FirewaterResource res;

    static
    {
        try {
            res = new FirewaterResource();
        } catch (Throwable ex) {
            throw Util.newInternal(ex);
        }
    }

    //~ Methods ----------------------------------------------------------------

    public static FwmPackage getFwmPackage(FarragoRepos repos)
    {
        RefPackage rp = repos.getFarragoPackage().refPackage("Fwm");
        return (FwmPackage) rp;
    }

    // implement FarragoSessionModelExtensionFactory
    public FarragoSessionModelExtension newModelExtension()
    {
        return new FirewaterModelExtension();
    }

    // override FarragoDbSessionFactory
    public FarragoSession newSession(
        String url,
        Properties info)
    {
        return new FirewaterSession(
            url,
            info,
            this);
    }

    // implement FarragoSessionSessionFactory
    public FarragoSessionPersonality newSessionPersonality(
        FarragoSession session,
        FarragoSessionPersonality defaultPersonality)
    {
        return new FirewaterSessionPersonality(
            (FarragoDbSession) session);
    }

    //~ Inner Classes ----------------------------------------------------------

    private static class FirewaterSession
        extends FarragoDbSession
    {
        FirewaterSession(
            String url,
            Properties info,
            FarragoSessionFactory sessionFactory)
        {
            super(url, info, sessionFactory);
        }

        protected void onEndOfTransaction(
            FarragoSessionTxnEnd eot)
        {
            super.onEndOfTransaction(eot);
            FirewaterDdlHandler.onEndOfTransaction(getRepos(), eot);
        }
    }

    private static class FirewaterSessionPersonality
        extends LucidDbSessionPersonality
    {
        protected FirewaterSessionPersonality(FarragoDbSession session)
        {
            super(session, null, false);
        }

        // implement FarragoSessionPersonality
        public String getDefaultLocalDataServerName(
            FarragoSessionStmtValidator stmtValidator)
        {
            return "SYS_FIREWATER_DATA_SERVER";
        }

        // implement FarragoSessionPersonality
        public boolean supportsFeature(ResourceDefinition feature)
        {
            // TODO jvs 9-May-2009:  review which other features
            // make sense in Firewater context

            EigenbaseResource featureResource = EigenbaseResource.instance();

            if (feature == featureResource.PersonalitySupportsLabels) {
                return true;
            }

            return super.supportsFeature(feature);
        }

        // implement FarragoSessionPersonality
        public void defineDdlHandlers(
            FarragoSessionDdlValidator ddlValidator,
            List<DdlHandler> handlerList)
        {
            FarragoRepos repos =
                ddlValidator.getStmtValidator().getSession().getRepos();

            // prevent drop of a node unless all partitions replicated
            // onto it have been dropped first
            ddlValidator.defineDropRule(
                getFwmPackage(repos).getDistributed().getNodeStoresPartition(),
                new FarragoSessionDdlDropRule(
                    "Node",
                    FemDataServer.class,
                    ReferentialRuleTypeEnum.IMPORTED_KEY_RESTRICT));

            handlerList.add(new FirewaterDdlHandler(ddlValidator));
            super.defineDdlHandlers(ddlValidator, handlerList);
        }

        // implement FarragoSessionPersonality
        public FarragoSessionParser newParser(FarragoSession session)
        {
            return new FirewaterFarragoParser();
        }
    }

    public static class FirewaterModelExtension
        implements FarragoSessionModelExtension
    {
        // implement FarragoSessionModelExtension
        public void defineDdlHandlers(
            FarragoSessionDdlValidator ddlValidator,
            List<DdlHandler> handlerList)
        {
            // REVIEW jvs 16-May-2009:  might be better to have the
            // Fwm-specific handlers registered here instead of
            // in session personality, although if session personality
            // is not always present, things are gonna get screwy,
            // so maybe not
        }

        // implement FarragoSessionModelExtension
        public void defineResourceBundles(
            List<ResourceBundle> bundleList)
        {
            bundleList.add(res);
        }

        // implement FarragoSessionModelExtension
        public void definePrivileges(
            FarragoSessionPrivilegeMap map)
        {
        }
    }

    public static class FirewaterFarragoParser extends FarragoAbstractParser
    {
        // implement FarragoAbstractParser
        protected FarragoAbstractParserImpl newParserImpl(Reader reader)
        {
            return new FirewaterParser(reader);
        }
    }
}

// End FirewaterSessionFactory.java
