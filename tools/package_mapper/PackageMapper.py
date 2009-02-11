import string,cgi
from datetime import datetime
from os import curdir, sep
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
from SocketServer import ThreadingMixIn

CONDOR_URL='http://www.cs.wisc.edu/condor/glidein/binaries/'
CORRAL_URL='http://www-rcf.usc.edu/~juve/glidein/'

class PackageMapper(HTTPServer,ThreadingMixIn):
	pass

class PackageMapperHandler(BaseHTTPRequestHandler):

	def log_message(self, format, *args):
		pass

	def do_GET(self):
		# Get requested package name
		package = self.path[1:]

		# Log the request
		date = datetime.utcnow().isoformat()+'Z'
		print date,self.client_address[0],package

		# Perform the mapping
		urls = self.map(package)

		# Send URLs back to client
		self.send_response(200)
		self.send_header('Content-type','text/plain')
		self.end_headers()
		for url in urls:
			self.wfile.write(url+'\n')

	def map(self, package):

		mappings = []

		# Process package
		if package.endswith('.tar.gz'):
			comp = package[:-7].split('-')
			if len(comp) == 5:
				# Parse the package components
				condor_version = comp[0]
				arch = comp[1]
				opsys = comp[2]
				opsys_version = comp[3]
				glibc_version = comp[4][5:]
				print condor_version,arch,opsys,opsys_version,glibc_version

		# Add default mappings
		mappings.append(CORRAL_URL+package)
		mappings.append(CONDOR_URL+package)

		return mappings

def main():
	try:
		server = PackageMapper(('',10960),PackageMapperHandler)
		print 'Started Package Mapper on port 10960...'
		server.serve_forever()
	except KeyboardInterrupt:
		print '^C received, shutting down server'
		server.socket.close()

if __name__ == '__main__':
	main()
