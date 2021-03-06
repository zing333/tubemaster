/*
 * TubeMaster++ - An Internet Multimedia Capture Tool.
 * Copyright (C) 2009 GgSofts
 * Contact: admin@tubemaster.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package Capture;


import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;


import Main.Commun;


public class TMPacket 
{
	private boolean 	isValid;
	private long 		ack;
	private long 		seq;
	private byte[] 		byteArray;
	private int 		srcPort;
	private int 		dstPort;

	
	public TMPacket(Packet p)
	{
		this.isValid = false;
		if (p instanceof TCPPacket)
		{
			TCPPacket pack = (TCPPacket) p;
			
			this.isValid 	= true;
			this.ack 		= pack.ack_num; 
			this.seq 		= pack.sequence;
			this.srcPort 	= pack.src_port;
			this.dstPort 	= pack.dst_port;
			this.byteArray 	= pack.data;
			
		}
		else this.isValid = false;				
	}

	
	public long 	getAck() 			{return this.ack;}	
	public long 	getSeq() 			{return this.seq;}	
	public byte[] 	getDatas() 			{return this.byteArray;}		
	public boolean 	isValid() 			{return this.isValid;}	
	public int 		getPorts() 			{return this.srcPort+this.dstPort;}
	
	
	public boolean contains(String str)
	{
		return (Commun.arrayPos(this.byteArray, str.getBytes(),1)>-1);
	}
	
	//=====================================================================================================
	
	public boolean searchFLV()
	{
		return (this.contains("Content-Type: video/x-flv") ||
				this.contains("FLV"+(char)1));
	}
	
	
	public boolean searchMP3()
	{
		return (this.contains("ID3"+(char)2) || 
				this.contains("ID3"+(char)3) || 
				this.contains("ID3"+(char)4) || 
				this.contains("Content-Type: audio/mpeg") ||
				(this.contains("LAME3.") && this.contains("Xing")));
	}
	
	
	public boolean searchMP4()
	{
		return (this.contains("ftypisom") || 
				this.contains("ftypmp4") || 
				this.contains("isomavc1"));
	}
	
	public boolean searchM4A()
	{
		return (this.contains("Content-Type: audio/mp4") ||
				this.contains("ftypm4a"));
	}
	
	
	public boolean searchMOV()
	{
		return (this.contains("ftypqt") || 
				this.contains("moov"));
	}
	
	
	public boolean searchFileHeader()
	{
		return (this.contains("Content-Length: ") ||
				this.contains("GET /"));
	}
	
	
	public boolean searchRTMP_Phase1()
	{
		return (this.contains((char)7+"connect"+(char)0+(char)0x3f));
	}
	
	public boolean searchRTMP_Phase2()
	{
		return (this.contains((char)4+"play"));
	}
	
	
	//=====================================================================================================	
		
	public String extractRTMPParameter(String key)
	{

		String ret = "";
		int pos = Commun.arrayPos(this.byteArray, key.getBytes(),1);
		
		//Intelligent auto-correction
		byte[] new_array = new byte[this.byteArray.length];
		int j = 0;
		for (int i=1;i<this.byteArray.length-1;i++)
		{
			int b = this.byteArray[i] & 0xff;
			int before = this.byteArray[i-1] & 0xff;
			int after = this.byteArray[i+1] & 0xff;
			boolean asup = (b > 126) && (before>31) && (before<123) && (after>31) && (after<123);
			if (!asup) 
			{
				new_array[j] = this.byteArray[i];
				j++;
			}

		}
		
		this.byteArray = new_array;
		pos = Commun.arrayPos(this.byteArray, key.getBytes(),1);

		while (this.byteArray[pos] != 2) 
		{	
			pos++;
			if (pos >= this.byteArray.length) return "";	
		}
		
		pos += 3;		
		int end = pos + ((this.byteArray[pos-1]&0xff)+((this.byteArray[pos-2]&0xff)*256));
			
		for (int i=pos;i<end;i++)
		{
			if (i < this.byteArray.length-1)
			{
				int b = this.byteArray[i] & 0xff;	
				boolean valid_char		= (b > 31) && (b < 126);
				boolean not_zero_before = (this.byteArray[i-1] != 0);
				boolean not_zero_after	= true;
				if (((i+1)<end)) if (this.byteArray[i+1] == 0) not_zero_after = false;
				
				if (valid_char && not_zero_after && not_zero_before) ret += (char) b;
				else end++;	
			}
		}

		return ret;
	}
		
	//=====================================================================================================	
	
	public void removeHTTPHeader()
	{		
		String str = new String(this.byteArray);
		if (str.indexOf("HTTP")==0)
		{
			int pos = 0;
			byte[] datas = this.byteArray;
			for(int i=0;i<datas.length-3;i++)
			{
				if ((datas[i]==13)&&(datas[i+1]==10)&&(datas[i+2]==13)&&(datas[i+3]==10))
				{
					pos = i + 4;
					break;	
				}
			}
			
			this.seq += pos;
			int size = datas.length-pos;
			byte[] newArray = new byte[size];
			for(int i=0;i<newArray.length;i++) newArray[i] = datas[pos+i];
			this.byteArray = newArray;	
		}
	}
	
	//=====================================================================================================	
	
	public long search_content_length()
	{
		String s = new String(this.byteArray);
		long size = -1;
		if (s.indexOf("Content-Length: ") > -1) 
			size = Integer.parseInt(Commun.parse(s,"Content-Length: ",""+(char)13));	
		else
		if (s.indexOf("Content-length: ") > -1) 
			size = Integer.parseInt(Commun.parse(s,"Content-length: ",""+(char)13));
		
		return size;
	}
	
	//=====================================================================================================	
	
	public String search_url()
	{
		String s = new String(this.byteArray);
		String url = "";
		String host = "";
		
		if ((s.indexOf("GET /") == 0) && (s.indexOf("crossdomain.xml") == -1))
		{
			url = Commun.parse(s, "GET /", " HTTP/");
			host = Commun.parse(s, "Host: ", ""+(char)13);	
		}
				
		return "http://"+host+"/"+url;	
	}
	
	//=====================================================================================================	

}
