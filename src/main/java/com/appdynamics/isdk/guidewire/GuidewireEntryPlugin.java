package com.appdynamics.isdk.guidewire;

import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.SDKClassMatchType;
import com.appdynamics.instrumentation.sdk.SDKStringMatchType;
import com.appdynamics.instrumentation.sdk.contexts.ISDKUserContext;
import com.appdynamics.instrumentation.sdk.logging.ISDKLogger;
import com.appdynamics.instrumentation.sdk.template.AEntry;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.IReflector;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.ReflectorException;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Created by louis on 13/06/18.
 */
public class GuidewireEntryPlugin extends AEntry implements FileChangeHandler {

    private IReflector invokeHttpReqGetHeader;
    private IReflector invokeHttpReqGetParameterMap;
    private AtomicBoolean mustLoadConfig = new AtomicBoolean( true );
    private List<BTServletMatch> btMatchRules ; // Do not access this variable directly, as it mey get overwritten on config reload
    private int c = 0 ;
    private Boolean debug = false ;

    private FileWatcher fw = null ;
    private String configFile = null ;

    public GuidewireEntryPlugin() {
        super();
        invokeHttpReqGetHeader = getNewReflectionBuilder().invokeInstanceMethod("getHeader", true, new String[] { String.class.getName() } ).build() ;
        invokeHttpReqGetParameterMap = getNewReflectionBuilder().invokeInstanceMethod("getParameterMap", true).build() ;
     }


    public void onFileChanged( )
    {
        mustLoadConfig.set( true );
    }


    @Override
    public String unmarshalTransactionContext(Object invokedObject,
                                              String className,
                                              String methodName,
                                              Object[] paramValues,
                                              ISDKUserContext isdkUserContext) throws ReflectorException
    {

        // check if we can find a singularity header. If so continue the transaction
        String header = null;
        try {
            header = (String) invokeHttpReqGetHeader.execute(paramValues[1].getClass().getClassLoader(),
                                                             paramValues[1], new Object[]{"singularityheader"});
        }
        catch( Exception ex )
        {
            getLogger().error("Error while looking for the correlation header\n", ex );
        }
        return header;
    }

    private boolean debugIsEnabled()
    {
        return getLogger().isDebugEnabled() || debug ;
    }

    private String getPluginPath()
    {
        StringBuilder sb = new StringBuilder() ;
        ISDKLogger l = getLogger() ;
        ProtectionDomain d = getClass().getProtectionDomain() ;

        String path = null ;
        if( d == null ) {
            if( debugIsEnabled() ) sb.append("No ProtectionDomain\n");
        }
        else {
            if( debugIsEnabled() ) sb.append( "found ProtectionDomain\n");
            CodeSource cc = d.getCodeSource();
            if (cc == null) {
                if( debugIsEnabled() )  sb.append("No Code Source\n");
            } else {
                if( debugIsEnabled() )  sb.append( "found CodeSource\n") ;
                URL url = cc.getLocation();
                if (url == null) {
                    if( debugIsEnabled() ) sb.append("No URL\n");
                } else {
                    if( debugIsEnabled() ) sb.append( "found Location\n");
                    path = url.getPath();
                    if( path == null ) {
                        if( debugIsEnabled() ) sb.append("Path not found\n");
                    }
                }
            }
        }

        if ( path == null ) // try alternative
        {
            if( debugIsEnabled() ) {
                sb.append("Trying alternative\n");
                sb.append("Class Name = " + getClass().getName() + "\n");
                sb.append("Canonical Name = " + getClass().getCanonicalName() + "\n");
            }

            String resource = getClass().getName().replace('.','/') + ".class" ;
            path = getClass().getClassLoader().getResource( resource ).getPath();
        }

        // Cleanup the file path
        path = path.split("!")[0].replaceAll("file:", "").replaceAll("GuidewireAgentPlugin\\.jar","") ;

        if( debugIsEnabled() ) {
            sb.append("Path = " + path + "\n");
            sb.append("=======================================================\n");
            getLogger().info(sb.toString()); // Log as info
        }

        return path ;
    }

    private synchronized List<BTServletMatch> getConfig()
    {
        // OK, make sure we only get one thread through here at a time...
        // Using the simples approach possible to deal with  multi-threaded
        // read-write access to the config, which can get changed on the fly.
        //
        // The idea is to have each thread get a reference to the current config by
        // returning a copy to the master reference held in btMatchRules. If a config reload
        // is needed, threads that are already in flight will complete with the old config,
        // while a new config-set is created.
        if ( mustLoadConfig.get() )
        {
            mustLoadConfig.set(false);

            getLogger().info("Loading Config");

            if (configFile == null)
                configFile = getPluginPath() + "GuidewireBTNames.csv";

            // If the file watcher to check for changes to the config file has not been started, do so now
            // TODO : Fix for java 1.6
            //if (fw == null) {
            //    getLogger().info("Starting File watcher on config file : " + configFile);
            //    fw = new FileWatcher(new File(configFile), this);
            //    fw.start();
            //}

            StringBuilder sb = new StringBuilder() ;
            BTConfigLoader loader = new BTConfigLoader(configFile, getLogger());
            getLogger().info( sb.toString() ) ;

            btMatchRules = loader.ParseConfig();
            debug=loader.enableDebug ;

            getLogger().info( "DEBUG = " + debug.toString() ) ;

            getLogger().info(new Integer(btMatchRules.size()).toString() + " match rules loaded");
        }
        return btMatchRules ;
    }

    @Override
    public String getBusinessTransactionName(Object invokedObject, String className, String methodName,
                                             Object[] paramValues, ISDKUserContext isdkUserContext) throws ReflectorException {

        String btName = "guidewire" ;

        StringBuilder sb = debugIsEnabled() ? new StringBuilder() : null ;

        try {
            // Keep a local reference to the current config, since a new version might be created on
            // a separate thread.
            List<BTServletMatch> config = getConfig();


            // get the query parameter map
            if( debugIsEnabled() ) sb.append("Getting parameter map\n");

            Map<String, String[]> params = (Map<String, String[]>) invokeHttpReqGetParameterMap.execute( paramValues[1].getClass().getClassLoader(),
                    paramValues[1], new Object[]{});

            // Print the parameters being matched
            if( debugIsEnabled() ) {
                sb.append("Num parameter = " + params.size() + "\n");

                for (String p : params.keySet()) {
                    if (debugIsEnabled() )
                        sb.append("   " + p + " : " + (params.get(p).length > 0 ? params.get(p)[0] : "") + "\n");
                }
            }

            // Try to match the request with one of the match rules
            if( debugIsEnabled() ) sb.append("attempting to find a match\n");


            BTServletMatch found = null;
            int ruleIndex = 0 ;
            int maxRules = config.size() ;
            while( ruleIndex < maxRules && ! (found=config.get(ruleIndex)).Match(params) )
                ruleIndex++ ;

            if( ruleIndex < maxRules )
                btName = found.btName;

            /*
            for (BTServletMatch btm : config) {
                if (btm.Match(params)) {
                    found = btm;
                    break;
                }
            }
            */

            if( debugIsEnabled() )
            {
                if ( btName == null)
                    sb.append("Request does not match a BT match rule\n");
                else
                    sb.append("Match found :" + found.toString() + "\n");
            }

        }
        catch (Exception ex )
        {
            getLogger().error("Error in getting BT name", ex );
        }

        if( debugIsEnabled() )
        {
            getLogger().info( sb.toString() );
            getLogger().info( "BT Name =" + btName ) ;
        }
        else {
            if (c++ > 200) {
                c = 0;
                getLogger().info("BT Name =" + btName);
            }
        }
        return btName ;
    }

    @Override
    public boolean isCorrelationEnabled() {
        return true;
    }

    @Override
    public boolean isCorrelationEnabledForOnMethodBegin() {
        return true;
    }

    @Override
    public List<Rule> initializeRules() {
        List<Rule> rules = new ArrayList<Rule>();
        Rule r = new Rule.Builder("com.guidewire.pl.web.controller.WebControllerImpl")
                .classStringMatchType(SDKStringMatchType.EQUALS)
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("processWithProfilingHouseKeeping")
                .methodStringMatchType(SDKStringMatchType.EQUALS)
                .build();
        rules.add(r) ;

        return rules;
    }
}
