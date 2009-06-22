<?php
$title = "Corral";
include_once("../../includes/header.inc");
?>

<h1>Corral</h1>

<table>
<tr>
<td valign=top width=500px>
<ul>
<li><a href="download.php">Download</a></li>
<li><a href="doc.php">Documentation</a></li>
<li><a href="../../support.php">Support</a></li>
</ul>

<p>Corral is a resource provisioning tool designed to help 
improve the performance of loosely-coupled applications on the grid.</p>

<p>The service is based on the concept of Condor "glideins." Glidein is 
a multi-level scheduling technique where Condor workers (called glideins) 
are submitted as user jobs via grid protocols to a remote cluster. The 
glideins are configured to contact a Condor central manager controlled 
by the user where they can be used to execute the user's jobs on the 
remote resources.</p>

<p>The use of glideins can significantly improve the performance of many 
irregular applications, like workflows, on grid resources. Using an 
application-specific scheduler allows users to fine-tune scheduling 
policies to fit the specific needs of their application. This can reduce 
or eliminate many of the scheduling overheads that occur when jobs are 
submitted using traditional grid mechanisms. It also allows users to 
allocate resources once, and reuse them for many jobs, which eliminates 
the queueing delays that are detrimental to applications with many 
fine-grained tasks.</p>
</td>

<td>
<img src="system.png" height=350px/>
</td>
</tr>
</table>

<?php
include_once("../../includes/footer.inc");
?>
