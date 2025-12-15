{{#brewRequireRelative}}
require_relative "{{.}}"
{{/brewRequireRelative}}

class {{brewFormulaName}} < Formula
  desc "{{projectDescription}}"
  homepage "{{projectLinkHomepage}}"
  url "{{distributionUrl}}"{{#brewDownloadStrategy}}, :using => {{.}}{{/brewDownloadStrategy}}
  version "{{projectVersion}}"
  sha256 "{{distributionChecksumSha256}}"
  license "{{projectLicense}}"
  head "https://{{repoHost}}/{{repoOwner}}/{{repoName}}.git", branch: "main"

  {{#brewHasLivecheck}}
  livecheck do
    {{#brewLivecheck}}
    {{.}}
    {{/brewLivecheck}}
  end
  {{/brewHasLivecheck}}

  {{! jdk dependency is not temurin because it's a cask and a formulae can't have a cask as dependency }}
  depends_on "{{jdkDistributionBrew}}@{{projectJavaVersionMajor}}"
  {{#brewDependencies}}
  depends_on {{.}}
  {{/brewDependencies}}

  def install

    # Install the software
    if build.head?
        # HEAD: install/build from the git repo
        # Install Tabulify Jars
        system "mvnw", "clean", "install", "-DskipTests"
        # Copy dependencies
        project = "{{cliProjectName}}"
        system "mvnw", "-Pdeps", "-pl", project
        # Assemble
        system "mvnw", "jreleaser:assemble", "-Djreleaser.config.file=jreleaser.yml", "-pl", project, "-Djreleaser.assemblers=javaArchive"
        # Install
        libexec.install Dir[project+"/target/jreleaser/assemble/tabulify-nojre/java-archive/work/tabulify-early-access-nojre/*"]
    else
        # Install from the zip
        libexec.install Dir["*"]
    end

    # Symlink
    bin.install_symlink "#{libexec}/bin/{{distributionExecutableUnix}}" => "{{distributionExecutableName}}"

    # Injecting JAVA_HOME in the header
    bin.children.each do |script|
        next unless script.file?
        original = File.read(script)
        modified = original.sub(
            /^#!\/usr\/bin\/env bash/,
            "#!/usr/bin/env bash\nJAVA_HOME=\"#{Formula["{{jdkDistributionBrew}}@{{projectJavaVersionMajor}}"].opt_prefix}\""
        )
        File.write(script, modified)
    end

  end

  # https://rubydoc.brew.sh/Formula#caveats-instance_method
  def caveats
    scripts_list = bin.children.map { |script| "  - #{script.basename}" }.join("\n")
    <<~EOS
      The following scripts have been installed:

      #{scripts_list}

    EOS
  end

  test do

    # `test do` will create, run in and delete a temporary directory.
    #
    # This test will fail, and we won't accept that! For Homebrew/homebrew-core
    # this will need to be a test that verifies the functionality of the
    # software. Run the test with `brew test dockenv`. Options passed
    # to `brew install` such as `--HEAD` also need to be provided to `brew test`.
    #
    # The installed folder is not in the path, so use the entire path to any
    # executables being tested: `system bin/"program", "do", "something"`.

    output = shell_output("#{bin}/{{distributionExecutableName}} --version")
    assert_match "{{projectVersion}}", output

  end

end
