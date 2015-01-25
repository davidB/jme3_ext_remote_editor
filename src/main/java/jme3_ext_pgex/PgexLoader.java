package jme3_ext_pgex;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.slf4j.LoggerFactory;

import pgex.Datas.Data;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.scene.Node;

public class PgexLoader implements AssetLoader {
	@Override
	public Object load(AssetInfo assetInfo) throws IOException {
		Pgex pgex = new Pgex(assetInfo.getManager());
		Node root = new Node(assetInfo.getKey().getName());
		InputStream in = null;
		LoggerCollector logger = new LoggerCollector("parse:"+ assetInfo.getKey().getName());
		try {
			in = assetInfo.openStream();
			Data src = Data.parseFrom(in, pgex.registry);
			pgex.merge(src, root, new HashMap<String, Object>(), logger);
		} finally {
			if (in != null){
				in.close();
			}
		}
		logger.dumpTo(LoggerFactory.getLogger(this.getClass()));
		//TODO check and transfert Lights on root if quantity == 1
		return (root.getQuantity() == 1)? root.getChild(0) : root;
	}

}
