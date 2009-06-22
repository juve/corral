<?php
$title = "Corral :: Documentation";
include_once("../../includes/header.inc");
?>

<h1>Corral :: Documentation</h1>

<h2>Presentations</h2>
<ul>
<li>"The Glidein Service" (<a href="glidein_service.ppt">ppt</a>, <a href="glidein_service.pdf">pdf</a>)</li>
</ul>

<h2>Client-Only Installation</h2>

<ol>
<li>Install Globus 4.2.x by following the instructions 
<a href="http://www.globus.org/toolkit/docs/4.2/">here</a>
(Note: Corral is no longer compatible with 4.0.x. If you need to use 
Globus 4.0.x use one of the older releases <a href="..">here</a>). 
You will only need the Globus client tools if you are only installing 
the Corral client.</li>
<li><a href="download.php">Download</a> the client-only GAR</li>
<li>Install the client with:
<pre>$ globus-deploy-gar glidein_service_client.gar</pre>
You will need to have the Globus tools on your path and have the environment 
variable $GLOBUS_LOCATION set. You may need to run this command as root depending 
on how you installed Globus.</li>
<li>(Optional) Set the environment variable $GLIDEIN_HOST to "juve.isi.edu". This
will cause the client to connect to the Glidein Service running on that host.</li>
<li>Test with:
<pre>
$ glidein help
$ glidein help ls
$ glidein ls
</pre>
</li>
</ol>

<h2>Service and Client Installation</h2>

<ol>
<li>Install Condor 7.0.x or later by following the instructions 
<a href="http://www.cs.wisc.edu/condor/manual/v7.2/3_2Installation.html">here</a></li>
<li>Install Globus 4.2.x by following the instructions 
<a href="http://www.globus.org/toolkit/docs/4.2/">here</a> (Note: Corral is no longer
compatible with 4.0.x. If you need to use Globus 4.0.x use one of the older releases 
<a href="..">here</a>). You will need a complete Globus WSRF container to run the service.</li>
<li><a href="download.php">Download</a> the combined client and service GAR</li>
<li>Deploy the Glidein Service using:
<pre>$ globus-deploy-gar glidein_service.gar</pre>
You will need to have the Globus tools on your path and have the environment
variable $GLOBUS_LOCATION set. You will probably need to run this command as
root depending on how you installed Globus.</li>
<li>Edit $GLOBUS_LOCATION/etc/glidein_service/jndi-config.xml. Make sure 
the condorHome configuration variable points to the directory where you 
installed Condor, and that condorConfig points to the Condor configuration
file condor_config. These paths should be absolute.</li>
<li>Restart the Globus container</li>
<li>Test with:
<pre>
$ glidein help
$ glidein help ls
$ glidein ls
</pre>
</li>
</ol>

<b>If you have trouble with installation contact: pegasus-support [at] isi [dot] edu</b>

<h2>Upgrading</h2>

<p>If you are upgrading from a previous version of the service you may need to apply
changes to the database. Check the  
<a href="https://pegasus.isi.edu/svn/glidein/trunk/RELEASE_NOTES">Release Notes</a>
and <a href="https://pegasus.isi.edu/svn/glidein/trunk/upgrade">upgrade directory</a>
in Subversion for more information.</p>

<h2>Getting Started</h2>

<p>If you have the Globus tools on your path ($GLOBUS_LOCATION/bin) then when you
install Corral you will automatically have the 'glidein' command-line tool on your
path. This is the only command you need to interact with Corral. The easiest way 
to find out about the 'glidein' command-line tool is to use the built-in help:
<pre>$ glidein help</pre>
</p>

<?php
include_once("../../includes/footer.inc");
?>
