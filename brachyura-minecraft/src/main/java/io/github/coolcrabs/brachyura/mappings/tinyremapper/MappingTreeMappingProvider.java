package io.github.coolcrabs.brachyura.mappings.tinyremapper;

import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodArgMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodVarMapping;
import net.fabricmc.tinyremapper.IMappingProvider;

public class MappingTreeMappingProvider implements IMappingProvider {
    private final MappingTree tree;
    private final int srcId;
    private final int dstId;

    public MappingTreeMappingProvider(MappingTree tree, String srcNamespace, String dstNamespace) {
        this.tree = tree;
        this.srcId = tree.getNamespaceId(srcNamespace);
        this.dstId = tree.getNamespaceId(dstNamespace);
    }

    @Override
    public void load(MappingAcceptor acceptor) {
        for (ClassMapping classMapping : tree.getClasses()) {
            if (classMapping == null) {
                continue; // Whatever
            }
            String classSrcName = classMapping.getName(srcId);
            String classDstName = classMapping.getName(dstId);
            if (classSrcName == null) {
                classSrcName = classDstName;
            }
            if (classDstName != null) {
                acceptor.acceptClass(classSrcName, classDstName);
            }
            for (MethodMapping method : classMapping.getMethods()) {
                if (method == null) {
                    continue; // Whatever
                }
                Member member = new Member(
                    classSrcName,
                    method.getName(srcId),
                    method.getDesc(srcId)
                );
                acceptor.acceptMethod(
                    member,
                    method.getName(dstId)
                );
                for (MethodArgMapping methodArgMapping : method.getArgs()) {
                    if (methodArgMapping == null) {
                        continue; // Whatever
                    }
                    String methodArgMappingDstName = methodArgMapping.getName(dstId);
                    if (methodArgMappingDstName != null) {
                        acceptor.acceptMethodArg(member, methodArgMapping.getLvIndex(), methodArgMappingDstName);
                    }
                }
                for (MethodVarMapping methodVarMapping : method.getVars()) {
                    if (methodVarMapping == null) {
                        continue; // Whatever
                    }
                    String methodVarMappingDstName = methodVarMapping.getName(dstId);
                    if (methodVarMappingDstName != null) {
                        acceptor.acceptMethodVar(member, methodVarMapping.getLvIndex(), methodVarMapping.getStartOpIdx(), methodVarMapping.getLvtRowIndex(), methodVarMappingDstName);
                    }
                }
            }
            for (FieldMapping field : classMapping.getFields()) {
                if (field == null) {
                    continue; // Whatever
                }
                acceptor.acceptField(
                    new Member(
                        classSrcName,
                        field.getName(srcId),
                        field.getDesc(srcId)
                    ),
                    field.getName(dstId)
                );
            }
        }
    }
    
}
