package org.springframework.roo.addon.json;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Metadata to be triggered by {@link RooJson} annotation
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public class JsonMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	
	// Constants
	private static final String PROVIDES_TYPE_STRING = JsonMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	
	// Fields
	private JsonAnnotationValues annotationValues;
	private String typeNamePlural;

	public JsonMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String typeNamePlural, JsonAnnotationValues annotationValues) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.hasText(typeNamePlural, "Plural of the target type required");
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		if (!isValid()) {
			return;
		}
		
		this.annotationValues = annotationValues;
		this.typeNamePlural = typeNamePlural;

		builder.addMethod(getToJsonMethod());
		builder.addMethod(getFromJsonMethod());
		builder.addMethod(getToJsonArrayMethod());
		builder.addMethod(getFromJsonArrayMethod());

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	public JavaSymbolName getToJsonMethodName() {
		String methodLabel = annotationValues.getToJsonMethod();
		if (methodLabel == null || methodLabel.length() == 0) {
			return null;
		}
		return new JavaSymbolName(methodLabel);
	}

	private MethodMetadata getToJsonMethod() {
		// Compute the relevant method name
		JavaSymbolName methodName = getToJsonMethodName();
		if (methodName == null) {
			return null;
		}

		// See if the type itself declared the method
		MethodMetadata result = MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, null);
		if (result != null) {
			return result;
		}

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String serializer = new JavaType("flexjson.JSONSerializer").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String root = annotationValues.getRootName() != null && annotationValues.getRootName().length() > 0 ? ".rootName(\"" + annotationValues.getRootName() + "\")" : "";
		bodyBuilder.appendFormalLine("return new " + serializer + "()" + root + ".exclude(\"*.class\")" + (annotationValues.isDeepSerialize() ? ".deepSerialize(this)" : ".serialize(this)") + ";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, new JavaType("java.lang.String"), bodyBuilder);
		methodBuilder.putCustomData(CustomDataJsonTags.TO_JSON_METHOD, null);
		return methodBuilder.build();
	}

	public JavaSymbolName getToJsonArrayMethodName() {
		String methodLabel = annotationValues.getToJsonArrayMethod();
		if (methodLabel == null || methodLabel.length() == 0) {
			return null;
		}
		return new JavaSymbolName(methodLabel);
	}

	private MethodMetadata getToJsonArrayMethod() {
		// Compute the relevant method name
		JavaSymbolName methodName = getToJsonArrayMethodName();
		if (methodName == null) {
			return null;
		}

		final JavaType parameterType = new JavaType(Collection.class.getName(), 0, DataType.TYPE, null, Arrays.asList(destination));

		// See if the type itself declared the method
		MethodMetadata result = getGovernorMethod(methodName, parameterType);
		if (result != null) {
			return result;
		}

		List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("collection"));

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String serializer = new JavaType("flexjson.JSONSerializer").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String root = annotationValues.getRootName() != null && annotationValues.getRootName().length() > 0 ? ".rootName(\"" + annotationValues.getRootName() + "\")" : "";
		bodyBuilder.appendFormalLine("return new " + serializer + "()" + root + ".exclude(\"*.class\")" + (annotationValues.isDeepSerialize() ? ".deepSerialize(collection)" : ".serialize(collection)") + ";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, new JavaType("java.lang.String"), AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		methodBuilder.putCustomData(CustomDataJsonTags.TO_JSON_ARRAY_METHOD, null);
		return methodBuilder.build();
	}

	public JavaSymbolName getFromJsonArrayMethodName() {
		String methodLabel = annotationValues.getFromJsonArrayMethod();
		if (methodLabel == null || methodLabel.length() == 0) {
			return null;
		}

		return new JavaSymbolName(methodLabel.replace("<TypeNamePlural>", typeNamePlural));
	}

	private MethodMetadata getFromJsonArrayMethod() {
		// Compute the relevant method name
		JavaSymbolName methodName = getFromJsonArrayMethodName();
		if (methodName == null) {
			return null;
		}
		
		final JavaType parameterType = JavaType.STRING;
		MethodMetadata result = getGovernorMethod(methodName, parameterType);
		if (result != null) {
			return result;
		}

		String list = new JavaType("java.util.List").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String arrayList = new JavaType("java.util.ArrayList").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		String bean = destination.getSimpleTypeName();

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String deserializer = new JavaType("flexjson.JSONDeserializer").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		bodyBuilder.appendFormalLine("return new " + deserializer + "<" + list + "<" + bean + ">>().use(null, " + arrayList + ".class).use(\"values\", " + bean + ".class).deserialize(json);");

		List<JavaSymbolName> parameterNames =  Arrays.asList(new JavaSymbolName("json"));

		JavaType collection = new JavaType("java.util.Collection", 0, DataType.TYPE, null, Arrays.asList(destination));

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, collection, AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		methodBuilder.putCustomData(CustomDataJsonTags.FROM_JSON_ARRAY_METHOD, null);
		return methodBuilder.build();
	}

	public JavaSymbolName getFromJsonMethodName() {
		String methodLabel = annotationValues.getFromJsonMethod();
		if (methodLabel == null || methodLabel.length() == 0) {
			return null;
		}

		// Compute the relevant method name
		return new JavaSymbolName(methodLabel.replace("<TypeName>", destination.getSimpleTypeName()));
	}

	private MethodMetadata getFromJsonMethod() {
		JavaSymbolName methodName = getFromJsonMethodName();
		if (methodName == null) {
			return null;
		}

		final JavaType parameterType = JavaType.STRING;
		MethodMetadata result = getGovernorMethod(methodName, parameterType);
		if (result != null) {
			return result;
		}

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String deserializer = new JavaType("flexjson.JSONDeserializer").getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver());
		bodyBuilder.appendFormalLine("return new " + deserializer + "<" + destination.getSimpleTypeName() + ">().use(null, " + destination.getSimpleTypeName() + ".class).deserialize(json);");

		List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("json"));

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC | Modifier.STATIC, methodName, destination,  AnnotatedJavaType.convertFromJavaTypes(parameterType), parameterNames, bodyBuilder);
		methodBuilder.putCustomData(CustomDataJsonTags.FROM_JSON_METHOD, null);
		return methodBuilder.build();
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static final String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
