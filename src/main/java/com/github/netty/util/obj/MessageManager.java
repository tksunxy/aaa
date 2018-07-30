package com.github.netty.util.obj;

import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Created by acer01 on 2018/7/29/029.
 */
public class MessageManager {

    /**
     * The ResourceBundle for this MessageManager.
     */
    private final ResourceBundle bundle;
    private final Locale locale;

    /**
     * Creates a new MessageManager for a given package. This is a
     * private method and all access to it is arbitrated by the
     * static getManager method call so that only one MessageManager
     * per package will be created.
     *
     * @param packageName Name of package to create MessageManager for.
     */
    private MessageManager(String packageName) {
        String bundleName = packageName + ".LocalStrings";
        ResourceBundle tempBundle = null;
        try {
            tempBundle = ResourceBundle.getBundle(bundleName, Locale.getDefault());
        } catch( MissingResourceException ex ) {
            // Try from the current loader (that's the case for trusted apps)
            // Should only be required if using a TC5 style classloader structure
            // where common != shared != server
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if( cl != null ) {
                try {
                    tempBundle = ResourceBundle.getBundle(
                            bundleName, Locale.getDefault(), cl);
                } catch(MissingResourceException ex2) {
                    // Ignore
                }
            }
        }
        // Get the actual locale, which may be different from the requested one
        if (tempBundle != null) {
            locale = tempBundle.getLocale();
        } else {
            locale = null;
        }
        bundle = tempBundle;
    }

    /**
     Get a string from the underlying resource bundle or return
     null if the String is not found.

     @param key to desired resource String
     @return resource String matching <i>key</i> from underlying
     bundle or null if not found.
     @throws IllegalArgumentException if <i>key</i> is null.
     */
    public String getString(String key) {
        if(key == null){
            String msg = "key may not have a null value";

            throw new IllegalArgumentException(msg);
        }

        String str = null;

        try {
            str = bundle.getString(key);
        } catch(MissingResourceException mre) {
            //bad: shouldn't mask an exception the following way:
            //   str = "[cannot find message associated with key '" + key + "' due to " + mre + "]";
            //     because it hides the fact that the String was missing
            //     from the calling code.
            //good: could just throw the exception (or wrap it in another)
            //      but that would probably cause much havoc on existing
            //      code.
            //better: consistent with container pattern to
            //      simply return null.  Calling code can then do
            //      a null check.
            str = null;
        }

        return str;
    }

    /**
     * Get a string from the underlying resource bundle and format
     * it with the given set of arguments.
     *
     * @param key  The key for the required message
     * @param args The values to insert into the message
     *
     * @return The request string formatted with the provided arguments or the
     *         key if the key was not found.
     */
    public String getString(final String key, final Object... args) {
        String value = getString(key);
        if (value == null) {
            value = key;
        }

        MessageFormat mf = new MessageFormat(value);
        mf.setLocale(locale);
        return mf.format(args, new StringBuffer(), null).toString();
    }

    // --------------------------------------------------------------
    // STATIC SUPPORT METHODS
    // --------------------------------------------------------------

    private static final Hashtable<String, MessageManager> managers =
            new Hashtable<>();

    /**
     * Get the MessageManager for a particular package. If a manager for
     * a package already exists, it will be reused, else a new
     * MessageManager will be created and returned.
     *
     * @param packageName The package name
     *
     * @return The instance associated with the given package
     */
    public static synchronized MessageManager getManager(String packageName) {
        MessageManager mgr = managers.get(packageName);
        if (mgr == null) {
            mgr = new MessageManager(packageName);
            managers.put(packageName, mgr);
        }
        return mgr;
    }


    public static MessageManager getManager(Class<?> clazz) {
        return getManager(clazz.getPackage().getName());
    }
}
