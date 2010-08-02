require 'net/http'
require 'rexml/document'

xml = ""
Net::HTTP.start('emoji4unicode.googlecode.com', 80) {|http|
	res = http.get('/svn/trunk/data/emoji4unicode.xml')
	xml = res.body
}

doc = REXML::Document.new xml

#file = File.new 'emoji4unicode.xml'
#doc = REXML::Document.new file


sbMap=Hash.new
auMap=Hash.new
gooMap=Hash.new
dcm2gooMap=Hash.new
dcm2sbMap=Hash.new

doc.each_element('//e'){|e|
	dcm = e.attributes['docomo']
	au = e.attributes['kddi']
	sb = e.attributes['softbank']
	goo = e.attributes['google']
	text = e.attributes['text_fallback']
	
	if sb!=nil
		val = dcm || text
		if sbMap[sb]==nil && val!=nil && sb[0,1]!='>'
			if val[0,1]=='>'
				val=val[1,100]
			end
			sbMap[sb] = val
		end
	end
	if au!=nil
		val = dcm || text
		if auMap[au]==nil && val!=nil && au[0,1]!='>'
			if val[0,1]=='>'
				val=val[1,100]
			end
			auMap[au] = val
		end
	end
	if goo!=nil
		val = dcm || text
		if gooMap[goo]==nil && val!=nil && goo[0,1]!='>'
			if val[0,1]=='>'
				val=val[1,100]
			end
			gooMap[goo] = val
		end
	end
	if dcm!=nil
		val = goo || text
		if dcm2gooMap[dcm]==nil && val!=nil && dcm[0,1]!='>'
			if val[0,1]=='>'
				val=val[1,100]
			end
			dcm2gooMap[dcm] = val
		end
	end
	if dcm!=nil
		val = sb || text
		if dcm2sbMap[dcm]==nil && val!=nil && dcm[0,1]!='>'
			if val[0,1]=='>'
				val=val[1,100]
			end
			dcm2sbMap[dcm] = val
		end
	end
}
now = Time.now
File.open('genSb2docomo.csv','w')do |f|
	f.puts "# #{now}"
	sbMap.sort.each do|key, value|
		f.puts "#{key},#{value}"
	end
end

File.open('genAu2docomo.csv','w')do |f|
	f.puts "# #{now}"
	auMap.sort.each do|key, value|
		f.puts "#{key},#{value}"
	end
end

File.open('genGoogle2docomo.csv','w')do |f|
	f.puts "# #{now}"
	gooMap.sort.each do|key, value|
		f.puts "#{key},#{value}"
	end
end

File.open('genDocomo2google.csv','w')do |f|
	f.puts "# #{now}"
	dcm2gooMap.sort.each do|key, value|
		f.puts "#{key},#{value}"
	end
end

File.open('genDocomo2sb.csv','w')do |f|
	f.puts "# #{now}"
	dcm2sbMap.sort.each do|key, value|
		f.puts "#{key},#{value}"
	end
end
