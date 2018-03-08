package com.itineric.bootstrapper;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Bootstrapper
{
  private static final int EXIT_NO_ARGS = -1;
  private static final int EXIT_UNKNOWN_ARG = -2;
  private static final int EXIT_MISSING_ARGUMENT = -5;
  private static final int EXIT_MANY_RUN_OPTION = -10;
  private static final int EXIT_NO_CLASS_TO_RUN = -100;

  private static final String HELP_OPTION = "-h";
  private static final String RUN_OPTION = "-r";
  private static final String LIBRARY_FOLDER_OPTION = "-l";
  private static final String CLASS_FOLDER_OPTION = "-c";
  private static final String TO_PROGRAM_OPTIONS = "--";

  public static void main(final String... args)
    throws Exception
  {
    if (args == null || args.length == 0)
    {
      printHelp();
      System.exit(EXIT_NO_ARGS);
    }

    boolean printHelp = false;
    String classToRunName = null;
    final List<URL> classPathUrls = new ArrayList<URL>();
    String[] programArguments = null;
    final AtomicInteger index = new AtomicInteger();
    for ( ; index.get() < args.length ; index.incrementAndGet())
    {
      final String arg = args[index.get()];
      if (HELP_OPTION.equals(arg))
      {
        printHelp = true;
      }
      else if (RUN_OPTION.equals(arg))
      {
        if (classToRunName != null)
        {
          System.exit(EXIT_MANY_RUN_OPTION);
        }
        classToRunName = getNextArgument(RUN_OPTION,
                                         index,
                                         args);
      }
      else if (LIBRARY_FOLDER_OPTION.equals(arg))
      {
        final String libraryFoldersAsString =
          getNextArgument(LIBRARY_FOLDER_OPTION,
                          index,
                          args);
        final List<String> libraryFolders = splitPaths(libraryFoldersAsString);

        for (final String libFolder : libraryFolders)
        {
          final File folder = new File(libFolder);
          loadJarFiles(classPathUrls, folder);
        }
      }
      else if (CLASS_FOLDER_OPTION.equals(arg))
      {
        final String classFoldersAsString =
          getNextArgument(CLASS_FOLDER_OPTION,
                          index,
                          args);
        final List<String> classFolders = splitPaths(classFoldersAsString);

        for (final String classFolder : classFolders)
        {
          final File folder = new File(classFolder);
          classPathUrls.add(folder.toURI().toURL());
        }
      }
      else if (TO_PROGRAM_OPTIONS.equals(arg))
      {
        programArguments = Arrays.copyOfRange(args, index.incrementAndGet(), args.length);
        break;
      }
      else
      {
        System.out.println("Unknown argument [" + arg + "]");
        System.exit(EXIT_UNKNOWN_ARG);
      }
    }

    if (printHelp)
    {
      printHelp();
    }

    if (!printHelp && classToRunName == null)
    {
      System.exit(EXIT_NO_CLASS_TO_RUN);
    }

    final URL[] urls = classPathUrls.toArray(new URL[classPathUrls.size()]);
    final URLClassLoader classLoader = new URLClassLoader(urls);
    Thread.currentThread().setContextClassLoader(classLoader);
    try
    {
      final Class<?> classToRun = Class.forName(classToRunName, true, classLoader);
      final Method mainMethod = classToRun.getMethod("main", String[].class);
      final Object[] arguments = new Object[1];
      arguments[0] = programArguments;
      mainMethod.invoke(null, arguments);
    }
    finally
    {
      classLoader.close();
    }
  }

  private static void printHelp()
  {
    System.out.println("Usage:");
    System.out.println("  -h               : Prints this help");
    System.out.println("  -r <main class>  : Main class to run");
    System.out.println("  -l <lib folder>  : Specify lib folder (this option can be provided many times)");
    System.out.println("                     Can given many paths, separate them using ':' (unix) or ';' (windows)");
    System.out.println("  -c <class folder>: Specify one class or resource folder "
                          + "(this option can be provided many times)");
    System.out.println("                     Can given many paths, separate them using ':' (unix) or ';' (windows)");
    System.out.println("  --               : All arguments after this one will be provided as given "
                         + "to the called main");
    System.out.println("");
    System.out.println("Example:");
    System.out.println("  > java -jar itineric-bootstrapper.jar -l lib -r com.itineric.App -- -a param1 -b param2");
    System.out.println("    Loads all jars inside 'lib' folder then calls "
                         + "com.itineric.App.main(\"-a\", \"param1\", \"-b\", \"param2\")");
  }

  private static String getNextArgument(final String option,
                                        final AtomicInteger index,
                                        final String... args)
  {
    final int i = index.incrementAndGet();
    if (i >= args.length)
    {
      System.out.println("Missing argument after [" + option + "]");
      printHelp();
      System.exit(EXIT_MISSING_ARGUMENT);
    }
    return args[i];
  }

  private static List<String> splitPaths(final String pathsAsString)
  {
    final String[] array = pathsAsString.split(File.pathSeparator);
    return Arrays.asList(array);
  }

  private static void loadJarFiles(final List<URL> jarFiles,
                                   final File folder)
    throws MalformedURLException
  {
    final List<File> subFolders = new ArrayList<File>();
    for (final File file : folder.listFiles())
    {
      if (file.isDirectory())
      {
        subFolders.add(file);
      }
      else
      {
        final String name = file.getName();
        if (name.endsWith(".jar")
          || name.endsWith(".zip"))
        {
          jarFiles.add(file.toURI().toURL());
        }
      }
    }

    for (final File subFolder : subFolders)
    {
      loadJarFiles(jarFiles, subFolder);
    }
  }
}
