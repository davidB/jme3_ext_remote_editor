package jme3_ext_pgex;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;

import pgex.Datas.Data;
import pgex_ext.Customparams;
import pgex_ext.Customparams.CustomParam;
import pgex_ext.Customparams.CustomParams;

import com.google.protobuf.ExtensionRegistry;
import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.IntMap;
import com.jme3.util.TangentBinormalGenerator;

// TODO use a Validation object (like in scala/scalaz) with option to log/dump stacktrace
@RequiredArgsConstructor
public class Pgex {
	final AssetManager assetManager;
	final Material defaultMaterial;
	public final ExtensionRegistry registry;

	public Pgex(AssetManager assetManager) {
		this.assetManager = assetManager;
		registry = setupExtensionRegistry(ExtensionRegistry.newInstance());
		defaultMaterial = newDefaultMaterial();
	}

	protected ExtensionRegistry setupExtensionRegistry(ExtensionRegistry r) {
		Customparams.registerAllExtensions(r);
		return r;
	}

	protected Material newDefaultMaterial() {
		Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", ColorRGBA.Gray);
		return m;
	}

	public Vector2f cnv(pgex.Datas.Vec2 src, Vector2f dst) {
		dst.set(src.getX(), src.getY());
		return dst;
	}

	public Vector3f cnv(pgex.Datas.Vec3 src, Vector3f dst) {
		dst.set(src.getX(), src.getY(), src.getZ());
		return dst;
	}

	public Vector4f cnv(pgex.Datas.Vec4 src, Vector4f dst) {
		dst.set(src.getX(), src.getY(), src.getZ(), src.getW());
		return dst;
	}

	public Quaternion cnv(pgex.Datas.Quaternion src, Quaternion dst) {
		dst.set(src.getX(), src.getY(), src.getZ(), src.getW());
		return dst;
	}

	public Vector4f cnv(pgex.Datas.Quaternion src, Vector4f dst) {
		dst.set(src.getX(), src.getY(), src.getZ(), src.getW());
		return dst;
	}

	public ColorRGBA cnv(pgex.Datas.Color src, ColorRGBA dst) {
		dst.set(src.getR(), src.getG(), src.getB(), src.getA());
		return dst;
	}

	public Matrix4f cnv(pgex.Datas.Mat4 src, Matrix4f dst) {
		dst.m00 = src.getC00();
		dst.m10 = src.getC10();
		dst.m20 = src.getC20();
		dst.m30 = src.getC30();
		dst.m01 = src.getC01();
		dst.m11 = src.getC11();
		dst.m21 = src.getC21();
		dst.m31 = src.getC31();
		dst.m02 = src.getC02();
		dst.m12 = src.getC12();
		dst.m22 = src.getC22();
		dst.m32 = src.getC32();
		dst.m03 = src.getC03();
		dst.m13 = src.getC13();
		dst.m23 = src.getC23();
		dst.m33 = src.getC33();
		return dst;
	}

	public Mesh.Mode cnv(pgex.Datas.Mesh.Primitive v) {
		switch(v) {
		case line_strip: return Mode.LineStrip;
		case lines: return Mode.Lines;
		case points: return Mode.Points;
		case triangle_strip: return Mode.TriangleStrip;
		case triangles: return Mode.Triangles;
		default: throw new IllegalArgumentException(String.format("doesn't support %s : %s", v.getClass(), v));
		}
	}

	public VertexBuffer.Type cnv(pgex.Datas.VertexArray.Attrib v) {
		switch(v) {
		case position: return Type.Position;
		case normal: return Type.Normal;
		case bitangent: return Type.Binormal;
		case tangent: return Type.Tangent;
		case color: return Type.Color;
		case texcoord: return Type.TexCoord;
		case texcoord2: return Type.TexCoord2;
		case texcoord3: return Type.TexCoord3;
		case texcoord4: return Type.TexCoord4;
		case texcoord5: return Type.TexCoord5;
		case texcoord6: return Type.TexCoord6;
		case texcoord7: return Type.TexCoord7;
		case texcoord8: return Type.TexCoord8;
		default: throw new IllegalArgumentException(String.format("doesn't support %s : %s", v.getClass(), v));
		}
	}

	//TODO use an optim version: including a patch for no autoboxing : https://code.google.com/p/protobuf/issues/detail?id=464
	public float[] hack_cnv(pgex.Datas.FloatBuffer src) {
		float[] b = new float[src.getValuesCount()];
		List<Float> l = src.getValuesList();
		for(int i = 0; i < b.length; i++) b[i] = l.get(i);
		return b;
	}

	//TODO use an optim version: including a patch for no autoboxing : https://code.google.com/p/protobuf/issues/detail?id=464
	public int[] hack_cnv(pgex.Datas.UintBuffer src) {
		int[] b = new int[src.getValuesCount()];
		List<Integer> l = src.getValuesList();
		for(int i = 0; i < b.length; i++) b[i] = l.get(i);
		return b;
	}

	public Mesh cnv(pgex.Datas.Mesh src, Mesh dst, Logger log) {
		if (src.getIndexArraysCount() > 1) {
			throw new IllegalArgumentException("doesn't support more than 1 index array");
		}
		if (src.getLod() > 1) {
			throw new IllegalArgumentException("doesn't support lod > 1 : "+ src.getLod());
		}

		dst.setMode(cnv(src.getPrimitive()));
		for(pgex.Datas.VertexArray va : src.getVertexArraysList()) {
			VertexBuffer.Type type = cnv(va.getAttrib());
			dst.setBuffer(type, va.getFloats().getStep(), hack_cnv(va.getFloats()));
			log.debug("add {}", dst.getBuffer(type));
		}
		for(pgex.Datas.IndexArray va : src.getIndexArraysList()) {
			dst.setBuffer(VertexBuffer.Type.Index, va.getInts().getStep(), hack_cnv(va.getInts()));
		}
		// basic check
		int nbVertices = dst.getBuffer(VertexBuffer.Type.Position).getNumElements();
		for(IntMap.Entry<VertexBuffer> evb : dst.getBuffers()) {
			if (evb.getKey() != VertexBuffer.Type.Index.ordinal()) {
				if (nbVertices != evb.getValue().getNumElements()) {
					log.warn("size of vertex buffer {} is not equals to vertex buffer for position: {} != {}", VertexBuffer.Type.values()[evb.getKey()], evb.getValue().getNumElements(), nbVertices);
				}
			}
		}
		//TODO optimize lazy create Tangent when needed (for normal map ?)
		if (dst.getBuffer(VertexBuffer.Type.Tangent) == null && dst.getBuffer(VertexBuffer.Type.TexCoord) != null) {
			TangentBinormalGenerator.generate(dst);
		}

		dst.updateCounts();
		dst.updateBound();
		return dst;
	}

	public Geometry cnv(pgex.Datas.Geometry src, Geometry dst, Logger log) {
		if (src.getMeshesCount() > 1) {
			throw new IllegalArgumentException("doesn't support more than 1 mesh");
		}
		dst.setName(src.hasName()?src.getName():src.getId());
		dst.setMesh(cnv(src.getMeshes(0), new Mesh(), log));
		return dst;
	}

	//TODO optimize to create less intermediate node
	public void merge(pgex.Datas.Data src, Node root, Map<String, Object> components, Logger log) {
		mergeTObjects(src, root, components, log);
		mergeGeometries(src, root, components, log);
		mergeMaterials(src, components, log);
		mergeLights(src, root, components, log);
		mergeCustomParams(src, components, log);
		// relations should be the last because it reuse data provide by other (put in components)
		mergeRelations(src, root, components, log);
	}

	private void mergeCustomParams(Data src, Map<String, Object> components, Logger log) {
		for(pgex_ext.Customparams.CustomParams srccp : src.getExtension(pgex_ext.Customparams.customParams)) {
			//TODO merge with existing
			components.put(srccp.getId(), srccp);
		}
	}

	public void mergeLights(Data src, Node root, Map<String, Object> components, Logger log) {
		for(pgex.Datas.Light srcl : src.getLightsList()) {
			//TODO manage parent hierarchy
			String id = srcl.getId();
			PgexLightControl dst = (PgexLightControl) components.get(id);
			if (dst == null) {
				dst = new PgexLightControl();
				components.put(id, dst);
				root.addControl(dst);
			}
			if (dst.light != null) {
				root.removeLight(dst.light);
			}
			dst.light = makeLight(srcl, log);
			root.addLight(dst.light);

			if (srcl.hasColor()) {
				dst.light.setColor(cnv(srcl.getColor(), new ColorRGBA()).mult(srcl.getIntensity()));
			}
			//TODO manage attenuation
			//TODO manage conversion of type
			switch(srcl.getKind()) {
			case spot: {
				SpotLight l = (SpotLight)dst.light;
				if (srcl.hasSpotAngle()) {
					float max = srcl.getSpotAngle().getMax();
					switch(srcl.getSpotAngle().getCurveCase()) {
						case CURVE_NOT_SET: {
							l.setSpotOuterAngle(max);
							l.setSpotInnerAngle(max);
							break;
						}
						case LINEAR: {
							l.setSpotOuterAngle(max * srcl.getSpotAngle().getLinear().getEnd());
							l.setSpotInnerAngle(max * srcl.getSpotAngle().getLinear().getBegin());
							break;
						}
						default: {
							l.setSpotOuterAngle(max);
							l.setSpotInnerAngle(max);
							log.warn("doesn't support curve like {} for spot_angle", srcl.getSpotAngle().getCurveCase());
						}
					}

				}
				if (srcl.hasRadialDistance()) {
					l.setSpotRange(srcl.getRadialDistance().getMax());
				}
				break;
			}
			case point: {
				PointLight l = (PointLight)dst.light;
				if (srcl.hasRadialDistance()) {
					float max = srcl.getRadialDistance().getMax();
					switch(srcl.getRadialDistance().getCurveCase()) {
					case CURVE_NOT_SET: {
						l.setRadius(max);
						break;
					}
					case LINEAR: {
						l.setRadius(max * srcl.getSpotAngle().getLinear().getEnd());
						break;
					}
					case SMOOTH: {
						l.setRadius(max * srcl.getSpotAngle().getSmooth().getEnd());
						break;
					}
					default: {
						l.setRadius(max);
						log.warn("doesn't support curve like {} for spot_angle", srcl.getSpotAngle().getCurveCase());
					}
					}
				}
				break;
			}
			default:
				//nothing
				break;
			}
		}
	}

	public Light makeLight(pgex.Datas.Light srcl, Logger log) {
		Light l0 = null;
		switch(srcl.getKind()) {
		case ambient:
			l0 = new AmbientLight();
			break;
		case directional: {
			DirectionalLight l = new DirectionalLight();
			l0 = l;
			break;
		}
		case spot: {
			SpotLight l = new SpotLight();
			l.setSpotRange(1000);
			l.setSpotInnerAngle(5f * FastMath.DEG_TO_RAD);
			l.setSpotOuterAngle(10f * FastMath.DEG_TO_RAD);
			l0 = l;
			break;
		}
		case point:
			l0 = new PointLight();
			break;
		}
		l0.setColor(ColorRGBA.White.mult(2));
		l0.setName(srcl.hasName()?srcl.getName():srcl.getId());
		return l0;
	}

	public void mergeTObjects(pgex.Datas.Data src, Node root, Map<String, Object> components, Logger log) {
		for(pgex.Datas.TObject n : src.getTobjectsList()) {
			//TODO manage parent hierarchy
			String id = n.getId();
			Spatial child = (Spatial) components.get(id);
			if (child == null) {
				child = new Node("");
				root.attachChild(child);
				components.put(id, child);
			}
			child.setName(n.hasName() ? n.getName():n.getId());
			if (n.getTransformsCount() > 0) {
				if (n.getTransformsCount() > 1) {
					throw new IllegalArgumentException("doesn't support more than 1 transform");
				}
				merge(n.getTransforms(0), child, log);
			}
		}
	}

	public void mergeGeometries(pgex.Datas.Data src, Node root, Map<String, Object> components, Logger log) {
		for(pgex.Datas.Geometry g : src.getGeometriesList()) {
			//TODO manage parent hierarchy
			String id = g.getId();
			Object o = components.get(id);
			Geometry child = (o instanceof Node)? toGeometry((Node)o) : (Geometry) o;
			if (child == null) {
				child = new Geometry();
				child.setMaterial(defaultMaterial);
				root.attachChild(child);
				components.put(id, child);
			}
			child = cnv(g, child, log);
			System.out.println("merge geometry : " + id);
		}
	}

	public Geometry toGeometry(Node src) {
		Geometry dst = new Geometry();
		dst.setName(src.getName());
		dst.setBatchHint(src.getBatchHint());
		dst.setCullHint(src.getLocalCullHint());
		dst.setLocalTransform(src.getLocalTransform());
		LightList ls = src.getLocalLightList();
		for(Light l: ls) {
			dst.addLight(l);
		}
		ls.clear();
		for(String k : src.getUserDataKeys()) {
			dst.setUserData(k, src.getUserData(k));
		}
		return dst;
	}
	public void mergeMaterials(pgex.Datas.Data src, Map<String, Object> components, Logger log) {
		for(pgex.Datas.Material m : src.getMaterialsList()) {
			//TODO manage parent hierarchy
			String id = m.getId();
			Material mat = (Material)components.get(id);
			//if (mat == null) {
				mat = newMaterial(m, log);
				components.put(id, mat);
			//}
			mat.setName(m.hasName() ? m.getName():m.getId());
			mergeToMaterial(m, mat, log);
		}
	}

	public void mergeRelations(pgex.Datas.Data src, Node root, Map<String, Object> components, Logger log) {
		for(pgex.Datas.Relation r : src.getRelationsList()) {
			Object op1 = components.get(r.getRef1());
			Object op2 = components.get(r.getRef2());
			if (op1 == null) {
				log.warn("can't link op1 not found : {}", r.getRef1());
			}
			if (op2 == null) {
				log.warn("can't link op2 not found : {}", r.getRef2());
			}
			if (op1 == null || op2 == null) continue;
			boolean done = false;
			if (op1 instanceof CustomParams) { // <--> pgex_ext.Customparams.CustomParams
				CustomParams cp1 = (CustomParams) op1;
				if (op2 instanceof Spatial) { // Geometry, Node
					for(CustomParam p : cp1.getParamsList()) {
						mergeToUserData(p, (Spatial) op2, log);
					}
					done = true;
				}
			}else if (op1 instanceof Geometry) { // <--> pgex.Datas.Geometry
				Geometry g1 = (Geometry) op1;
				if (op2 instanceof PgexLightControl) {
					PgexLightControl l2 = (PgexLightControl)op2;
					l2.getSpatial().removeControl(l2);
					g1.addControl(l2);
					// TODO raise an alert, strange to link LightNode and Geometry
					done = true;
				} else if (op2 instanceof Material) {
					g1.setMaterial((Material)op2);
					done = true;
				} else if (op2 instanceof Node) {
					((Node) op2).attachChild(g1);
					done = true;
				}
			} else if (op1 instanceof PgexLightControl) { // <--> pgex.Datas.Light
				PgexLightControl l1 = (PgexLightControl)op1;
				if (op2 instanceof Node) {
					l1.getSpatial().removeControl(l1);
					((Node) op2).addControl(l1);
					done = true;
				}
			} else if (op1 instanceof Material) { // <--> pgex.Datas.Material
				Material m1 = (Material)op1;
				if (op2 instanceof Node) {
					((Node) op2).setMaterial(m1);
					done = true;
				}
			} else if (op1 instanceof Node) { // <--> pgex.Datas.Node
			}
			if (!done) {
				log.warn("doesn't know how to make relation {}({}) -- {}({})\n", r.getRef1(), op1.getClass(), r.getRef2(), op2.getClass());
			}
		}
	}

	public Spatial mergeToUserData(CustomParam p, Spatial dst, Logger log) {
		String name = p.getName();
		switch(p.getValueCase()) {
		case VALUE_NOT_SET:
			dst.setUserData(name, null);
			break;
		case VBOOL:
			dst.setUserData(name, p.getVbool());
			break;
		case VCOLOR:
			dst.setUserData(name, cnv(p.getVcolor(), new ColorRGBA()));
			break;
		case VFLOAT:
			dst.setUserData(name, p.getVfloat());
			break;
		case VINT:
			dst.setUserData(name, p.getVint());
			break;
		case VMAT4:
			dst.setUserData(name, cnv(p.getVmat4(), new Matrix4f()));
			break;
		case VQUAT:
			dst.setUserData(name, cnv(p.getVquat(), new Vector4f()));
			break;
		case VSTRING:
			dst.setUserData(name, p.getVstring());
			break;
		case VTEXTURE:
			dst.setUserData(name, getValue(p.getVtexture(), log));
			break;
		case VVEC2:
			dst.setUserData(name, cnv(p.getVvec2(), new Vector2f()));
			break;
		case VVEC3:
			dst.setUserData(name, cnv(p.getVvec3(), new Vector3f()));
			break;
		case VVEC4:
			dst.setUserData(name, cnv(p.getVvec4(), new Vector4f()));
			break;
		default:
			log.warn("Material doesn't support parameter : {} of type {}", name, p.getValueCase().name());
			break;
		}
		return dst;
	}

	public Image.Format getValue(pgex.Datas.Texture2DInline.Format f, Logger log) {
		switch(f){
		//case bgra8: return Image.Format.BGR8;
		case rgb8: return Image.Format.RGB8;
		case rgba8: return Image.Format.RGBA8;
		default: throw new UnsupportedOperationException("image format :" + f);
		}
	}

	public Texture getValue(pgex.Datas.Texture t, Logger log) {
		switch(t.getDataCase()){
		case DATA_NOT_SET: return null;
		case RPATH: {
			Texture tex = assetManager.loadTexture(t.getRpath());
			return tex;
		}
		case TEX2D: {
			pgex.Datas.Texture2DInline t2di = t.getTex2D();
			Image img = new Image(getValue(t2di.getFormat(), log), t2di.getWidth(), t2di.getHeight(), t2di.getData().asReadOnlyByteBuffer());
			return new Texture2D(img);
		}
		default:
			throw new IllegalArgumentException("doesn't support more than texture format:" + t.getDataCase());
		}
	}

	public Material newMaterial(pgex.Datas.Material m, Logger log) {
		boolean lightFamily = !m.getShadeless();
		String def = lightFamily ? "Common/MatDefs/Light/Lighting.j3md" : "Common/MatDefs/Misc/Unshaded.j3md";
		Material mat = new Material(assetManager, def);
		if (lightFamily) {
			mat.setBoolean("UseMaterialColors", true);
			mat.setBoolean("UseVertexColor", true);
		}
		return mat;
	}

	public Material mergeToMaterial(pgex.Datas.Material src, Material dst, Logger log) {
		MaterialDef md = dst.getMaterialDef();
		setColor(src.hasColor(), src.getColor(), dst, new String[]{"Color", "Diffuse"}, md, log);
		setTexture2D(src.hasColorMap(), src.getColorMap(), dst, new String[]{"ColorMap", "DiffuseMap"}, md, log);
		//setTexture2D(src.hasNormalMap(), src.getNormalMap(), dst, new String[]{"ColorMap", "DiffuseMap"}, md, log);
		setFloat(src.hasOpacity(), src.getOpacity(), dst, new String[]{"Alpha", "Opacity"}, md, log);
		setTexture2D(src.hasOpacityMap(), src.getOpacityMap(), dst, new String[]{"AlphaMap", "OpacityMap"}, md, log);
		setTexture2D(src.hasNormalMap(), src.getNormalMap(), dst, new String[]{"NormalMap"}, md, log);
		setFloat(src.hasRoughness(), src.getRoughness(), dst, new String[]{"Roughness"}, md, log);
		setTexture2D(src.hasRoughnessMap(), src.getRoughnessMap(), dst, new String[]{"RoughnessMap"}, md, log);
		setFloat(src.hasMetalness(), src.getMetalness(), dst, new String[]{"Metalness"}, md, log);
		setTexture2D(src.hasMetalnessMap(), src.getMetalnessMap(), dst, new String[]{"MetalnessMap"}, md, log);
		setColor(src.hasSpecular(), src.getSpecular(), dst, new String[]{"Specular"}, md, log);
		setTexture2D(src.hasSpecularMap(), src.getSpecularMap(), dst, new String[]{"SpecularMap"}, md, log);
		setFloat(src.hasSpecularPower(), src.getSpecularPower(), dst, new String[]{"SpecularPower", "Shininess"}, md, log);
		setTexture2D(src.hasSpecularPowerMap(), src.getSpecularPowerMap(), dst, new String[]{"SpecularPowerMap", "ShininessMap"}, md, log);
		setColor(src.hasEmission(), src.getEmission(), dst, new String[]{"Emission", "GlowColor"}, md, log);
		setTexture2D(src.hasEmissionMap(), src.getEmissionMap(), dst, new String[]{"EmissionMap", "GlowMap"}, md, log);
		return dst;
	}

	public void setColor(boolean has, pgex.Datas.Color src, Material dst, String[] names, MaterialDef scope, Logger log){
		if (has) {
			String name = findMaterialParamName(names, VarType.Vector4, scope, log);
			if (name != null) {
				dst.setColor(name, cnv(src, new ColorRGBA()));
			} else {
				log.warn("can't find a matching name for : [{}] ({})", String.join(",", names), VarType.Vector4);
			}
		}
	}

	public void setTexture2D(boolean has, pgex.Datas.Texture src, Material dst, String[] names, MaterialDef scope, Logger log){
		if (has) {
			String name = findMaterialParamName(names, VarType.Texture2D, scope, log);
			if (name != null) {
				dst.setTexture(name, getValue(src, log));
			} else {
				log.warn("can't find a matching name for : [{}] ({})", String.join(",", names), VarType.Texture2D);
			}
		}
	}

	public void setFloat(boolean has, float src, Material dst, String[] names, MaterialDef scope, Logger log){
		if (has) {
			String name = findMaterialParamName(names, VarType.Float, scope, log);
			if (name != null) {
				dst.setFloat(name, src);
			} else {
				log.warn("can't find a matching name for : [{}] ({})", String.join(",", names), VarType.Float);
			}
		}
	}

	public String findMaterialParamName(String[] names, VarType type, MaterialDef scope, Logger log) {
		for(String name2 : names){
			for(MatParam mp : scope.getMaterialParams()) {
				if (mp.getName().equalsIgnoreCase(name2) && mp.getVarType() == type) {
					return mp.getName();
				}
			}
		}
		return null;
	}

	public void merge(pgex.Datas.Transform src, Spatial dst, Logger log) {
		dst.setLocalRotation(cnv(src.getRotation(), dst.getLocalRotation()));
		dst.setLocalTranslation(cnv(src.getTranslation(), dst.getLocalTranslation()));
		dst.setLocalScale(cnv(src.getScale(), dst.getLocalScale()));
	}
}

