/* Delete any existing tables */
drop table if exists interface;
drop table if exists execution_service;
drop table if exists glidein;
drop table if exists environment;
drop table if exists site;

/**
 * Table to keep track of the version of this database. It contains the version
 * of the database and the date that it was created.
 */
CREATE TABLE interface (
	revision	TEXT NOT NULL,
	created		DATETIME NOT NULL
);

INSERT INTO interface (revision,created) VALUES ('$Revision: 483 $',datetime('now'));

/**
 * Table to store information about sites. This includes any data needed
 * to help install the condor executables at the site and any current status
 * information.
 */
CREATE TABLE site ( 
	id				INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,	-- Site ID number
	name			TEXT NOT NULL,		-- The name of the site
	installPath 	TEXT NOT NULL,		-- Path where executables are kept
	localPath		TEXT NOT NULL,		-- Path where local files are kept (logs, etc.)
	condorPackage	TEXT,				-- Name of the condor package (e.g. 7.0.0-x86_64-Linux-glibc2.3)
	condorVersion	TEXT,				-- Version of condor to install (e.g. 7.0.0)
	state			TEXT NOT NULL,		-- The current state (e.g. READY)
	shortMessage	TEXT NOT NULL,		-- A short status message
	longMessage		TEXT,				-- A long status message (e.g. Stack trace)
	submitted		DATETIME NOT NULL,	-- Time when site was submitted
	lastUpdate		DATETIME NOT NULL	-- Time when site was last updated
);

/**
 * Each site has a list of built-in environment variables that will be
 * included in each job submitted for that site.
 */
CREATE TABLE environment (
	site			INTEGER NOT NULL,	-- Site ID
	variable		TEXT NOT NULL,		-- The name of the variable (e.g. PATH)
	value			TEXT,				-- The value of the variable
	PRIMARY KEY (site, variable)
);

/**
 * Each site has two kinds of execution services: STAGING and GLIDEIN that
 * specify how jobs are submitted to the site. Staging is used for site setup
 * jobs, and glidein is used for glidein jobs. Right now only GT2 and GT4 are
 * supported, but in the future any Condor job type could be supported.
 */
CREATE TABLE execution_service (
	site			INTEGER NOT NULL,	-- Site ID
	function		TEXT NOT NULL,		-- Use for staging or glideins
	serviceContact	TEXT NOT NULL,		-- The URL of the grid service
	serviceType		TEXT NOT NULL,		-- The version of grid service
	project			TEXT,				-- The desired project name/id
	queue			TEXT,				-- The target queue name/id
	PRIMARY KEY (site, function)
);

/**
 * Table to keep track of glideins. Each site has zero or more running
 * glideins.
 */
CREATE TABLE glidein (
	id				INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,	-- Glidein ID
	site			INTEGER,			-- Site ID
	condorHost		TEXT NOT NULL,		-- Host where condor CM runs
	count			INTEGER NOT NULL,	-- Total number of processes
	hostCount		INTEGER NOT NULL,	-- Total number of hosts
	wallTime		INTEGER NOT NULL,	-- Runtime in minutes
	numCpus			INTEGER NOT NULL,	-- Num CPUs / process
	condorConfig	TEXT,				-- Custom config file (base 64 encoded)
	gcbBroker		TEXT,				-- The GCB broker IP address
	idleTime		INTEGER,			-- Idle time in minutes
	condorDebug		TEXT,				-- Debugging flags for condor daemons
	state			TEXT NOT NULL,		-- The current state of the glidein (e.g. RUNNING)
	shortMessage	TEXT NOT NULL,		-- Short status message
	longMessage		TEXT,				-- Long status message (e.g. stack trace)
	submitted		DATETIME NOT NULL,	-- Date when glidein was submitted
	lastUpdate		DATETIME NOT NULL	-- Date when glidein was last updated
);