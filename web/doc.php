<?php
$title = "Glidein Service :: Documentation";
include_once("../../includes/header.inc");
?>

<h1>Glidein Service :: Documentation</h1>

<h2>Presentations</h2>
<p>The Glidein Service <a href="glidein_service.ppt">ppt</a> <a href="glidein_service.pdf">pdf</a></p>

<h2>Client-Only Installation</h2>

<p>Procedure:</p>
<ol>
<li>Install Globus 4.0.x by following the instructions 
<a href="http://www.globus.org/toolkit/docs/4.0/">here</a> 
(has not been tested with Globus 4.2.x). You will only need the 
Globus client tools.</li>
<li><a href="download.php">Download</a> the Glidein Service client</li>
<li>Install the client with:
<pre>$ globus-deploy-gar glidein_service_client_&lt;version&gt;.gar</pre>
where &lt;version&lt; is the version you downloaded. You will need to have
the Globus tools on your path and have the environment variable $GLOBUS_LOCATION
set. You may need to run this command as root depending on how you installed 
Globus.</li>
<li>Set the environment variable $GLIDEIN_HOST to juve.isi.edu (or whatever server you are going to connect to)</li>
<li>Test with:
<pre>$ glidein help
$ glidein help ls
$ glidein ls
$ glidein ls -authz none</pre>
</li>
</ol>

<h2>Service and Client Installation</h2>

<ol>
<li>Install Condor 7.0.x or later by following the instructions 
<a href="http://www.cs.wisc.edu/condor/manual/v7.2/3_2Installation.html">here</a></li>
<li>Install Globus 4.0.x by following the instructions 
<a href="http://www.globus.org/toolkit/docs/4.0/">here</a> 
(has not been tested with Globus 4.2.x).</li>
<li><a href="download.php">Download</a> the Glidein Service</li>
<li>Deploy the Glidein Service using:
<pre>$ globus-deploy-gar glidein_service_&lt;version&gt;.gar</pre>
where &lt;version&lt; is the version you downloaded. You will need to have
the Globus tools on your path and have the environment variable $GLOBUS_LOCATION
set. You will probably need to run this command as root depending on how you
installed the Globus container.</li>
<li>Edit: $GLOBUS_LOCATION/etc/glidein_service/jndi-config.xml. Make sure 
the condorHome configuration variable points to the directory where you 
installed Condor, and that condorConfig points to the Condor configuration
file condor_config. These paths should be absolute.</li>
<li>Restart the Globus container</li>
<li>Test with:
<pre>$ glidein help
$ glidein help ls
$ glidein ls
$ glidein ls -authz none</pre>
</li>
</ol>

<b>If you have problems contact: pegasus-support [at] isi [dot] edu</b>

<h2>Getting Started</h2>

<p>If you have the Globus tools on your path ($GLOBUS_LOCATION/bin) then when you
install the Glidein Service you will automatically have the 'glidein' command-line
tool on your path. This is the only command you need to interact with the Glidein
Service. The easiest way to find out about the 'glidein' command-line tool is to 
use the built-in help:
<pre>glidein help</pre>
</p>

<?php
include_once("../../includes/footer.inc");
?>
